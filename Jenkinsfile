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
        // Stage 2: Maven 编译 + 打包（含单元测试）
        // =============================================
        stage('Maven Build & Test') {
            steps {
                sh '''
                    mvn clean package \
                        -DskipTests=false \
                        -Dtest="!MallPortalApplicationTests,!PortalProductDaoTests" \
                        -DfailIfNoTests=false \
                        -Dsurefire.failIfNoSpecifiedTests=false \
                        -pl mall-portal,mall-admin \
                        -am
                '''
            }
            post {
                always {
                    junit allowEmptyResults: true,
                        testResults: '**/target/surefire-reports/*.xml'
                }
                failure {
                    error '编译或单元测试失败，流水线终止！'
                }
            }
        }

        // =============================================
        // Stage 3: SonarQube 代码扫描
        // =============================================
        stage('SonarQube Scan') {
            steps {
                withSonarQubeEnv('sonarqube-server') {
                    sh '''
                        mvn sonar:sonar \
                            -Dsonar.projectKey=mall-master \
                            -Dsonar.branch.name=${BRANCH_NAME} \
                            -Dsonar.java.binaries=**/target/classes \
                            -DskipTests=true
                    '''
                }
            }
        }

        // =============================================
        // Stage 4: SonarQube 质量门禁
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
        // Stage 5: 构建 Docker 镜像
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
        // Stage 6: 部署测试环境
        // =============================================
        stage('Deploy to Test') {
            steps {
                sh '''
                    cd ${WORKSPACE}
                    # 用新镜像重启服务（--no-build 避免重新构建）
                    docker compose up -d --no-build --force-recreate mall-portal mall-admin
                    echo "测试环境部署完成，等待服务启动..."
                    sleep 20
                '''
            }
        }

        // =============================================
        // Stage 7: 冒烟测试
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
