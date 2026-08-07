pipeline {
    agent any

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
                echo "当前分支: ${env.BRANCH_NAME}"
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
                            -Dsonar.branch.name=${BRANCH_NAME}
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
        // Stage 6: 冒烟测试
        // =============================================
        // stage('Smoke Test') {
        //     steps {
        //         sh '''
        //             echo "检查 mall-portal 健康状态..."
        //             curl -f --max-time 15 http://mall-portal:8085/actuator/health || exit 1

        //             echo "检查 mall-admin 健康状态..."
        //             curl -f --max-time 15 http://mall-admin:8080/actuator/health || exit 1

        //             echo "✅ 冒烟测试通过！"
        //         '''
        //     }
        //     post {
        //         failure {
        //             echo '❌ 冒烟测试失败，请检查服务日志！'
        //         }
        //     }
        // }
    }

    post {
        success {
            echo '========================================'
            echo '✅ 流水线执行成功！'
            echo "   分支: ${env.BRANCH_NAME}"
            echo "   构建号: ${env.BUILD_NUMBER}"
            echo '   请进行人工验证后合并到 develop'
            echo '========================================'
        }
        failure {
            echo '========================================'
            echo '❌ 流水线执行失败！'
            echo "   分支: ${env.BRANCH_NAME}"
            echo '   请检查 Jenkins 日志排查问题'
            echo '========================================'
        }
    }
}
