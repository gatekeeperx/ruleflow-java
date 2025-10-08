@Library('devops-pipelines-libraries') _

def flow = new com.gatekeeperx.util();

pipeline {
    agent { node { label '!master' } }

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '15'))
    }

    stages {

        stage('CI_SONAR') {
            when {
                anyOf {
                    branch 'develop'
                    buildingTag()
                    changeRequest()
                }
            }
            steps {
                script {
                    flow.init()
                  
                    flow.wstage("Build", {
                        flow.dockerBuild();
                    })


                    flow.wstage("Test", {
                      sh 'docker-compose run --rm test'
                    })
          
                    flow.wstage("Teardown", {
                      sh 'docker-compose down -v'
                    })
                  
                    flow.wstage("Coverage", {
                        sh './gradlew jacocoTestCoverageVerification'
                        jacoco(execPattern: 'build/jacoco/jacocoTest.exec')
                        junit 'build/test-results/*.xml'
                    })

                    flow.wstage("Sonar", {
                      flow.sonarAnalysis();
                    })

                    flow.wstage("Quality Gate", {
                        timeout(time: 1, unit: 'HOURS') {
                            def qg = waitForQualityGate()
                            if (qg.status != 'OK') {
                            error "Pipeline aborted due to quality gate failure: ${qg.status}"
                            }
                        }
                    })
                }
            }
        }

        stage('push to ECR') {
          when { buildingTag()}
          steps {
            script{
                flow.dockerPush();
            }
          }
        }

        stage("End") {
            steps {
                script {
                    sh "echo DONE"
                }
            }
        }
    }
  
    post {
        always {
            sh "sudo chmod -R 777 ."
            cleanWs()
            deleteDir() /* clean up our workspace */
        }
    }
}