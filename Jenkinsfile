
pipeline {
    agent any

    options {
        timestamps()
        skipDefaultCheckout(true)

    }

    environment {
        COMPOSE_FILE = 'docker-compose.ci.yml'
        MVN_ARGS = '-U -B'
        BASE_URL = 'http://app:8080'
        SELENIUM_URL = 'http://localhost:14444/wd/hub'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout(
                    changelog: false,
                    poll: false,
                    scm: [
                        $class: 'GitSCM',
                        branches: [[name: '*/main']],
                        userRemoteConfigs: [[url: 'https://github.com/RuhanKarimov/eHaliSaha.git']]
                    ]
                )
            }
        }



        stage('2-Build') {
            steps {
                script {
                    if (isUnix()) {
                        sh "./mvnw ${env.MVN_ARGS} -DskipTests package"
                    } else {
                        bat ".\\mvnw.cmd %MVN_ARGS% -DskipTests package"
                    }
                }
            }
        }

        stage('3-Unit Tests') {
            steps {
                script {
                    if (isUnix()) {
                        sh "./mvnw ${env.MVN_ARGS} -DskipUnitTests=false -DskipITs=true test"
                    } else {
                        bat ".\\mvnw.cmd %MVN_ARGS% -DskipUnitTests=false -DskipITs=true test"
                    }
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('4-Integration Tests') {
            steps {
                script {
                    if (isUnix()) {
                        sh "./mvnw ${env.MVN_ARGS} -DskipUnitTests=true -DskipITs=false verify"
                    } else {
                        bat ".\\mvnw.cmd %MVN_ARGS% -DskipUnitTests=true -DskipITs=false verify"
                    }
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/failsafe-reports/*.xml'
                }
            }
        }

        stage('5-Run System on Docker') {
            steps {
                bat 'docker compose -f %COMPOSE_FILE% up -d --build'
                sleep time: 20, unit: 'SECONDS'
            }
        }

        stage('6.1-E2E (Selenium) Scenario 1: Owner Login') {
            steps {
                script {
                    if (isUnix()) {
                        sh "BASE_URL=${env.BASE_URL} SELENIUM_URL=${env.SELENIUM_URL} ./mvnw ${env.MVN_ARGS} -P e2e -Dtest=Scenario01OwnerLoginTestE2E test"
                    } else {
                        bat "set BASE_URL=%BASE_URL%&& set SELENIUM_URL=%SELENIUM_URL%&& .\\mvnw.cmd %MVN_ARGS% -P e2e -Dtest=Scenario01OwnerLoginTestE2E test"
                    }
                }
            }
        }

        stage('6.2-E2E Scenario 2: Owner Creates Facility') {
            steps {
                script {
                    if (isUnix()) {
                        sh "BASE_URL=${env.BASE_URL} SELENIUM_URL=${env.SELENIUM_URL} ./mvnw ${env.MVN_ARGS} -P e2e -Dtest=Scenario02OwnerCreateFacilityTestE2E test"
                    } else {
                        bat "set BASE_URL=%BASE_URL%&& set SELENIUM_URL=%SELENIUM_URL%&& .\\mvnw.cmd %MVN_ARGS% -P e2e -Dtest=Scenario02OwnerCreateFacilityTestE2E test"
                    }
                }
            }
        }

        stage('6.3-E2E Scenario 3: Owner Setup Slots Pitch Pricing') {
            steps {
                script {
                    if (isUnix()) {
                        sh "BASE_URL=${env.BASE_URL} SELENIUM_URL=${env.SELENIUM_URL} ./mvnw ${env.MVN_ARGS} -P e2e -Dtest=Scenario03OwnerSetupTestE2E test"
                    } else {
                        bat "set BASE_URL=%BASE_URL%&& set SELENIUM_URL=%SELENIUM_URL%&& .\\mvnw.cmd %MVN_ARGS% -P e2e -Dtest=Scenario03OwnerSetupTestE2E test"
                    }
                }
            }
        }

        stage('6.4-E2E Scenario 4: Member Login') {
            steps {
                script {
                    if (isUnix()) {
                        sh "BASE_URL=${env.BASE_URL} SELENIUM_URL=${env.SELENIUM_URL} ./mvnw ${env.MVN_ARGS} -P e2e -Dtest=Scenario04MemberLoginTestE2E test"
                    } else {
                        bat "set BASE_URL=%BASE_URL%&& set SELENIUM_URL=%SELENIUM_URL%&& .\\mvnw.cmd %MVN_ARGS% -P e2e -Dtest=Scenario04MemberLoginTestE2E test"
                    }
                }
            }
        }

        stage('6.5-E2E Scenario 5: Member Sends Membership Request') {
            steps {
                script {
                    if (isUnix()) {
                        sh "BASE_URL=${env.BASE_URL} SELENIUM_URL=${env.SELENIUM_URL} ./mvnw ${env.MVN_ARGS} -P e2e -Dtest=Scenario05MemberSendMembershipRequestTestE2E test"
                    } else {
                        bat "set BASE_URL=%BASE_URL%&& set SELENIUM_URL=%SELENIUM_URL%&& .\\mvnw.cmd %MVN_ARGS% -P e2e -Dtest=Scenario05MemberSendMembershipRequestTestE2E test"
                    }
                }
            }
        }

        stage('6.6-E2E Scenario 6: Owner Approves Membership') {
            steps {
                script {
                    if (isUnix()) {
                        sh "BASE_URL=${env.BASE_URL} SELENIUM_URL=${env.SELENIUM_URL} ./mvnw ${env.MVN_ARGS} -P e2e -Dtest=Scenario06OwnerApproveMembershipRequestTestE2E test"
                    } else {
                        bat "set BASE_URL=%BASE_URL%&& set SELENIUM_URL=%SELENIUM_URL%&& .\\mvnw.cmd %MVN_ARGS% -P e2e -Dtest=Scenario06OwnerApproveMembershipRequestTestE2E test"
                    }
                }
            }
        }

        stage('6.7-E2E Scenario 7: Member Makes Reservation') {
            steps {
                script {
                    if (isUnix()) {
                        sh "BASE_URL=${env.BASE_URL} SELENIUM_URL=${env.SELENIUM_URL} ./mvnw ${env.MVN_ARGS} -P e2e -Dtest=Scenario07MemberMakesReservationTestE2E test"
                    } else {
                        bat "set BASE_URL=%BASE_URL%&& set SELENIUM_URL=%SELENIUM_URL%&& .\\mvnw.cmd %MVN_ARGS% -P e2e -Dtest=Scenario07MemberMakesReservationTestE2E test"
                    }
                }
            }
        }

        stage('6.8-E2E Scenario 8: Double Booking Should Fail') {
            steps {
                script {
                    if (isUnix()) {
                        sh "BASE_URL=${env.BASE_URL} SELENIUM_URL=${env.SELENIUM_URL} ./mvnw ${env.MVN_ARGS} -P e2e -Dtest=Scenario08DoubleBookingShouldFailTestE2E test"
                    } else {
                        bat "set BASE_URL=%BASE_URL%&& set SELENIUM_URL=%SELENIUM_URL%&& .\\mvnw.cmd %MVN_ARGS% -P e2e -Dtest=Scenario08DoubleBookingShouldFailTestE2E test"
                    }
                }
            }
        }

        stage('6.9-E2E Scenario 9: Owner Sees Reservation List') {
            steps {
                script {
                    if (isUnix()) {
                        sh "BASE_URL=${env.BASE_URL} SELENIUM_URL=${env.SELENIUM_URL} ./mvnw ${env.MVN_ARGS} -P e2e -Dtest=Scenario09OwnerSeesReservationInListTestE2E test"
                    } else {
                        bat "set BASE_URL=%BASE_URL%&& set SELENIUM_URL=%SELENIUM_URL%&& .\\mvnw.cmd %MVN_ARGS% -P e2e -Dtest=Scenario09OwnerSeesReservationInListTestE2E test"
                    }
                }
            }
        }

        stage('6.10-E2E Scenario 10: Membership Already Active Should Fail') {
            steps {
                script {
                    if (isUnix()) {
                        sh "BASE_URL=${env.BASE_URL} SELENIUM_URL=${env.SELENIUM_URL} ./mvnw ${env.MVN_ARGS} -P e2e -Dtest=Scenario10MembershipAlreadyActiveShouldFailTestE2E test"
                    } else {
                        bat "set BASE_URL=%BASE_URL%&& set SELENIUM_URL=%SELENIUM_URL%&& .\\mvnw.cmd %MVN_ARGS% -P e2e -Dtest=Scenario10MembershipAlreadyActiveShouldFailTestE2E test"
                    }
                }
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'e2e-reports/*.xml'
            archiveArtifacts artifacts: 'target/*.jar', allowEmptyArchive: true

            script {
                if (isUnix()) {
                    sh "docker compose -f ${env.COMPOSE_FILE} down -v || true"
                } else {
                    bat "docker compose -f %COMPOSE_FILE% down -v"
                }
            }
        }
    }
}
