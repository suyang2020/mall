pipeline {
    agent any

    parameters {
        booleanParam(name: 'SKIP_API_TEST', defaultValue: false, description: '跳过接口自动化测试')
        choice(name: 'API_ENV', choices: ['test', 'prod', 'demo'], description: '测试环境')
        choice(name: 'API_SYSTEM',
            choices: ['全部', '示例系统', 'APP', '后台管理系统'],
            description: '测试系统（"全部"则执行所有系统）')
        choice(name: 'API_MODULE',
            choices: ['所有', '演示模块', '通用', '直播', '直播小黄车', '积分商城', '商城', '购物车',
                      '优惠券', '我的订单', '我的', '商户中心',
                      '直播管理', '商户管理', '商品管理', '兑换券管理', '订单管理',
                      '用户积分管理', '优惠券类型模块'],
            description: '测试模块（"所有"则执行该系统下全部模块）')
    }

    environment {
        SONAR_HOST_URL = 'http://sonarqube:9000'
        // 通知凭据（Jenkins → Manage Credentials 中配置）
        WECHAT_WEBHOOK_KEY = credentials('wechat-webhook-key')
        EMAIL_PASSWORD     = credentials('email-password')
    }

    stages {

        // =============================================
        // Stage 1: 代码检出
        // =============================================
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    // 普通 Pipeline 没有 BRANCH_NAME，从 Git 插件变量获取
                    // GIT_BRANCH 格式为 origin/feature/login，去掉 origin/ 前缀
                    env.GIT_BRANCH_NAME = env.GIT_BRANCH ?
                        env.GIT_BRANCH.replaceFirst('^origin/', '') : 'unknown'
                }
                echo "当前分支: ${env.GIT_BRANCH_NAME}"
                echo "提交ID: ${GIT_COMMIT.take(8)}"
            }
        }

        // =============================================
        // Stage 2: Maven 编译 + 测试 + SonarQube 扫描
        // （合并在一个 Maven 会话中，确保扫描时所有 class 文件已生成）
        // =============================================
        stage('Build & SonarQube Scan') {
            steps {
                withSonarQubeEnv('sonarqube-server') {
                    sh '''
                        mvn clean package sonar:sonar \
                            -DskipTests=false \
                            -Dtest="!MallPortalApplicationTests,!PortalProductDaoTests" \
                            -DfailIfNoTests=false \
                            -Dsurefire.failIfNoSpecifiedTests=false \
                            -pl mall-portal,mall-admin \
                            -am \
                            -Dsonar.projectKey=mall-master
                    '''
                }
            }
            post {
                always {
                    junit allowEmptyResults: true,
                        testResults: '**/target/surefire-reports/*.xml'
                }
                failure {
                    error '编译、测试或SonarQube扫描失败，流水线终止！'
                }
            }
        }

        // =============================================
        // Stage 3: SonarQube 质量门禁
        // =============================================
        stage('Quality Gate') {
            steps {
                script {
                    def qg = waitForQualityGate()
                    if (qg.status != 'OK') {
                        error "质量门禁未通过！状态: ${qg.status}"
                    }
                    echo '✅ 质量门禁通过！'
                }
            }
        }

        // =============================================
        // Stage 4: 构建 Docker 镜像
        // =============================================
        stage('Build Docker Images') {
            parallel {
                stage('mall-portal') {
                    steps {
                        sh '''
                            docker build \
                                -t mall/mall-portal:latest \
                                -t mall/mall-portal:${BUILD_NUMBER} \
                                -f mall-portal/Dockerfile mall-portal/
                        '''
                    }
                }
                stage('mall-admin') {
                    steps {
                        sh '''
                            docker build \
                                -t mall/mall-admin:latest \
                                -t mall/mall-admin:${BUILD_NUMBER} \
                                -f mall-admin/Dockerfile mall-admin/
                        '''
                    }
                }
            }
        }

        // =============================================
        // Stage 5: 部署测试环境
        // =============================================
        stage('Deploy to Test') {
            steps {
                sh '''
                    cd ${WORKSPACE}
                    # 先停掉旧容器再重建（不同 compose 项目无法接管已有容器）
                    docker stop mall-portal mall-admin || true
                    docker rm mall-portal mall-admin || true
                    docker-compose up -d --no-build --no-deps mall-portal mall-admin
                    echo "测试环境部署完成，等待服务启动..."
                    sleep 20
                '''
            }
        }

        // =============================================
        // Stage 6: 冒烟测试（快速验证服务是否存活）
        // =============================================
        stage('Smoke Test') {
            steps {
                sh '''
                    echo "检查 mall-portal 健康状态..."
                    curl -f --max-time 15 http://mall-portal:8085/actuator/health || exit 1

                    echo "检查 mall-admin 健康状态..."
                    curl -f --max-time 15 http://mall-admin:8080/actuator/health || exit 1

                    echo "✅ 冒烟测试通过！"
                '''
            }
            post {
                failure {
                    echo '❌ 冒烟测试失败，请检查服务日志！'
                }
            }
        }

        // =============================================
        // Stage 7: 接口自动化测试（Python + JMeter/Excel）
        // 使用 python:3.10-slim 容器运行，自带 pip/git，无需折腾系统环境
        // =============================================
        stage('API Automation Test') {
            agent {
                docker {
                    image 'python:3.10-slim'
                    args '-u root -v $HOME/.cache/pip:/root/.cache/pip'
                    reuseNode true
                }
            }
            when {
                expression { return !params.SKIP_API_TEST }
            }
            steps {
                script {
                    // 系统-模块映射校验
                    def validModules = [
                        '全部':     ['所有'],
                        '示例系统':  ['所有', '演示模块'],
                        'APP':      ['所有', '直播', '直播小黄车', '积分商城', '商城', '购物车', '优惠券', '我的订单', '我的', '商户中心'],
                        '后台管理系统': ['所有', '直播管理', '商户管理', '商品管理', '兑换券管理', '订单管理', '用户积分管理']
                    ]

                    def sys  = params.API_SYSTEM
                    def mod  = params.API_MODULE

                    if (sys != '全部' && (!validModules.containsKey(sys) || !validModules[sys].contains(mod))) {
                        error("模块 '${mod}' 不属于系统 '${sys}'，请重新选择")
                    }

                    def sysArg  = (sys == '全部') ? '' : sys
                    def modArg  = (sys == '全部' || mod == '所有') ? '' : mod

                    echo "接口测试参数: 系统=${sysArg ?: '全部'}  模块=${modArg ?: '全部'}  环境=${params.API_ENV}"

                    // 执行测试（失败不中断流水线，标记为 UNSTABLE）
                    try {
                        // slim 镜像不含 git，先装上
                        sh 'apt-get update -qq && apt-get install -y -qq --no-install-recommends git'

                        // 拉取测试仓库（使用 Jenkins 凭据）
                        withCredentials([usernamePassword(
                            credentialsId: 'github-credential',
                            usernameVariable: 'GIT_USER',
                            passwordVariable: 'GIT_PASS'
                        )]) {
                            sh '''
                                if [ -d "autoInterface/.git" ]; then
                                    echo "仓库已存在，拉取最新代码..."
                                    cd autoInterface && git fetch --depth 1 origin main && git reset --hard origin/main
                                else
                                    echo "首次克隆仓库..."
                                    git clone --depth 1 --branch main \
                                        "https://${GIT_USER}:${GIT_PASS}@github.com/suyang2020/autoInterface.git" \
                                        autoInterface
                                fi
                            '''
                        }

                        // 安装依赖（python:3.13-slim 容器自带 pip）
                        sh '''
                            cd autoInterface
                            if [ -f "requirements.txt" ]; then
                                pip install -r requirements.txt \
                                    -i https://pypi.tuna.tsinghua.edu.cn/simple
                            else
                                pip install -i https://pypi.tuna.tsinghua.edu.cn/simple \
                                    openpyxl requests lxml
                            fi
                        '''

                        // 生成 mall 代码变更 diff，供 Python 脚本做精准测试
                        sh '''
                            cd ${WORKSPACE}

                            if git rev-parse origin/develop >/dev/null 2>&1; then
                                echo "生成相对于 origin/develop 的 git diff..."
                                git diff --name-status origin/develop...HEAD -- . > autoInterface/gitdiff.txt
                            elif git rev-parse HEAD~1 >/dev/null 2>&1; then
                                echo "origin/develop 不可用，使用 HEAD~1..."
                                git diff --name-status HEAD~1 -- . > autoInterface/gitdiff.txt
                            else
                                echo "(无历史记录，生成空 diff)"
                                touch autoInterface/gitdiff.txt
                            fi

                            echo "===== 变更文件 (前30行) ====="
                            head -30 autoInterface/gitdiff.txt
                        '''

                        // 执行测试
                        sh """
                            cd autoInterface

                            JMETER_BIN=\$(find . -name jmeter -type f -path "*/bin/*" | head -1)
                            if [ -n "\$JMETER_BIN" ]; then
                                chmod +x "\$JMETER_BIN"
                            fi

                            python run/scheduler.py "${sysArg}" "${modArg}" "${params.API_ENV}" gitdiff.txt
                        """
                    } catch (Exception e) {
                        echo "接口测试执行失败: ${e.getMessage()}"
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
            post {
                always {
                    // HTML 测试报告
                    publishHTML([allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'autoInterface/report',
                        reportFiles: 'index.html',
                        reportName: 'API Test Report'
                    ])
                    // 归档产物
                    archiveArtifacts allowEmptyArchive: true,
                        artifacts: 'autoInterface/report/**, autoInterface/result/**'
                }
            }
        }
    }

    post {
        success {
            script {
                def msg = "## ✅ 构建成功\n" +
                    "> **项目**: mall-master\n" +
                    "> **分支**: ${env.GIT_BRANCH_NAME}\n" +
                    "> **构建号**: #${env.BUILD_NUMBER}\n" +
                    "> **提交**: ${env.GIT_COMMIT ? env.GIT_COMMIT.take(8) : 'N/A'}\n" +
                    "> [查看详情](${env.BUILD_URL})"

            }
        }
        failure {
            script {
                def msg = "## ❌ 构建失败\n" +
                    "> **项目**: mall-master\n" +
                    "> **分支**: ${env.GIT_BRANCH_NAME}\n" +
                    "> **构建号**: #${env.BUILD_NUMBER}\n" +
                    "> **提交**: ${env.GIT_COMMIT ? env.GIT_COMMIT.take(8) : 'N/A'}\n" +
                    "> [查看详情](${env.BUILD_URL})"

            }
         
        }
        unstable {
            // 接口测试失败时 Python 脚本已发送通知，此处不再重复
            echo "⚠️ 构建不稳定（UNSTABLE），请查看 API Test Report 了解详情"
        }
    }
}

