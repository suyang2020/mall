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
    }

    stages {

        // =============================================
        // Stage 1: 代码检出
        // =============================================
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    // 普通 Pipeline 没有 BRANCH_NAME，从 git 获取
                    env.GIT_BRANCH_NAME = sh(
                        script: 'git rev-parse --abbrev-ref HEAD',
                        returnStdout: true
                    ).trim()
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
                            -Dsonar.projectKey=mall-master \
                            -Dsonar.branch.name=${GIT_BRANCH_NAME}
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
        // =============================================
        stage('API Automation Test') {
            when {
                expression { return !params.SKIP_API_TEST }
            }
            steps {
                script {
                    // 拉取独立测试仓库
                    dir('autoInterface') {
                        git url: 'https://github.com/suyang2020/autoInterface.git',
                            branch: 'main',
                            credentialsId: 'github-cred'
                    }

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

                    // 安装 Python 依赖
                    sh '''
                        cd autoInterface
                        if [ -f "requirements.txt" ]; then
                            pip3 install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple
                        else
                            pip3 install -i https://pypi.tuna.tsinghua.edu.cn/simple openpyxl requests lxml
                        fi
                    '''

                    // 执行测试（失败不中断流水线，标记为 UNSTABLE）
                    try {
                        sh """
                            cd autoInterface

                            JMETER_BIN=\$(find . -name jmeter -type f -path "*/bin/*" | head -1)
                            if [ -n "\$JMETER_BIN" ]; then
                                chmod +x "\$JMETER_BIN"
                            fi

                            python3 run/scheduler.py "${sysArg}" "${modArg}" "${params.API_ENV}"
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
                    publishHTML([allowMissing: true,
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
            echo '========================================'
            echo '✅ 流水线执行成功！'
            echo "   分支: ${env.GIT_BRANCH_NAME}"
            echo "   构建号: ${env.BUILD_NUMBER}"
            echo '   请进行人工验证后合并到 develop'
            echo '========================================'
        }
        failure {
            echo '========================================'
            echo '❌ 流水线执行失败！'
            echo "   分支: ${env.GIT_BRANCH_NAME}"
            echo '   请检查 Jenkins 日志排查问题'
            echo '========================================'
        }
    }
}
