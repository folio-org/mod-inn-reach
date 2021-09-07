buildMvn {
  publishModDescriptor = 'yes'
  mvnDeploy = 'yes'
  doKubeDeploy = true
  buildNode = 'jenkins-agent-java11'

  doApiLint = true
  doApiDoc = true
  apiTypes = 'OAS'
  apiDirectories = 'src/main/resources/swagger.api'

  doDocker = {
    buildDocker {
      publishMaster = 'yes'
      healthTimeout = 20
      healthRetries = 6
      healthChk = true
      healthChkCmd = 'curl -sS --fail -o /dev/null http://localhost:8081/admin/health || exit 1'
    }
  }
}
