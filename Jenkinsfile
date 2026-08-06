pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = 'localhost:5000'
        MAVEN_HOME = tool name: 'maven-3.9', type: 'maven'
    }

    stages {

        // =============================================
        // Stage 1: 代码检出
        // =============================================
        stage('Checkout') {
            steps {
                checkout scm
                echo "当前分支: ${env.BRANCH_NAME}"
                echo "提交ID: ${env.GIT_COMMIT}"
            }
        }

        // =============================================
        // Stage 2: 编译 & 单元测试
        // =============================================
        stage('Compile & Unit Test') {
            steps {
                sh '''
                    mvn clean compile -DskipTests=false -pl mall-portal,mall-admin,mall-common,mall-mbg,mall-security -am
                '''
            }
            post {
                success {
                    junit '**/target/surefire-reports/*.xml'
                }
                failure {
                    echo '编译或单元测试失败，终止流水线！'
                    error 'Build failed at Compile & Unit Test stage'
                }
            }
        }

        // =============================================
        // Stage 3: Maven 单元测试
        // =============================================
        stage('Unit Tests') {
            steps {
                sh '''
                    mvn test -DskipTests=false \
                        -pl mall-portal,mall-admin \
                        -DfailIfNoTests=false
                '''
            }
            post {
                always {
                    junit allowEmptyResults: true,
                        testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        // =============================================
        // Stage 4: SonarQube 代码质量检测
        // =============================================
        stage('SonarQube Analysis') {
            environment {
                SONAR_HOST_URL = 'http://localhost:9000'
                SONAR_LOGIN = credentials('sonar-token')
            }
            steps {
                script {
                    if (env.BRANCH_NAME == 'main' || env.BRANCH_NAME.startsWith('release')) {
                        sh '''
                            mvn sonar:sonar \
                                -Dsonar.host.url=${SONAR_HOST_URL} \
                                -Dsonar.login=${SONAR_LOGIN} \
                                -Dsonar.projectKey=mall-master \
                                -Dsonar.branch.name=${BRANCH_NAME}
                        '''
                    } else {
                        echo "分支 ${env.BRANCH_NAME} 跳过 SonarQube 全量分析，执行本地代码扫描..."
                        sh '''
                            mvn sonar:sonar \
                                -Dsonar.host.url=${SONAR_HOST_URL} \
                                -Dsonar.login=${SONAR_LOGIN} \
                                -Dsonar.projectKey=mall-master \
                                -Dsonar.branch.name=${BRANCH_NAME}
                        '''
                    }
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
                            docker build -t ${DOCKER_REGISTRY}/mall-portal:${BUILD_NUMBER} \
                                -f mall-portal/Dockerfile .
                            docker tag ${DOCKER_REGISTRY}/mall-portal:${BUILD_NUMBER} \
                                ${DOCKER_REGISTRY}/mall-portal:latest
                        '''
                    }
                }
                stage('mall-admin') {
                    steps {
                        sh '''
                            docker build -t ${DOCKER_REGISTRY}/mall-admin:${BUILD_NUMBER} \
                                -f mall-admin/Dockerfile .
                            docker tag ${DOCKER_REGISTRY}/mall-admin:${BUILD_NUMBER} \
                                ${DOCKER_REGISTRY}/mall-admin:latest
                        '''
                    }
                }
            }
        }

        // =============================================
        // Stage 6: 推送镜像到仓库
        // =============================================
        stage('Push Docker Images') {
            steps {
                sh '''
                    docker push ${DOCKER_REGISTRY}/mall-portal:${BUILD_NUMBER}
                    docker push ${DOCKER_REGISTRY}/mall-portal:latest
                    docker push ${DOCKER_REGISTRY}/mall-admin:${BUILD_NUMBER}
                    docker push ${DOCKER_REGISTRY}/mall-admin:latest
                '''
            }
        }

        // =============================================
        // Stage 7: 部署测试环境
        // =============================================
        stage('Deploy to Test') {
            steps {
                sh '''
                    export BUILD_NUMBER=${BUILD_NUMBER}
                    cd /opt/mall-test
                    docker compose down
                    docker compose up -d
                    echo "测试环境部署完成，等待服务启动..."
                    sleep 30
                '''
            }
        }

        // =============================================
        // Stage 8: 冒烟测试（Smoke Test）
        // =============================================
        stage('Smoke Test') {
            steps {
                sh '''
                    # 检查服务健康状态
                    echo "检查 mall-portal 健康状态..."
                    curl -f --max-time 10 http://localhost:8085/actuator/health || exit 1

                    echo "检查 mall-admin 健康状态..."
                    curl -f --max-time 10 http://localhost:8080/actuator/health || exit 1

                    echo "冒烟测试通过！"
                '''
            }
        }
    }

    // =============================================
    // 后置操作：通知
    // =============================================
    post {
        success {
            echo '流水线执行成功！代码已部署到测试环境，请进行人工验证。'
            // 可配置发送邮件/钉钉/飞书通知
        }
        failure {
            echo '流水线执行失败！请检查日志排查问题。'
            // 可配置发送告警通知
        }
    }
}
