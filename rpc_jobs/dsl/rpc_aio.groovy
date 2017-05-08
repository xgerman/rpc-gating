// CIT Slave node
timeout(time: 8, unit: 'HOURS'){
  node() {
    dir("rpc-gating") {
        git branch: env.RPC_GATING_BRANCH, url: env.RPC_GATING_REPO
        common = load 'pipeline_steps/common.groovy'
        pubcloud = load 'pipeline_steps/pubcloud.groovy'
        aio_prepare = load 'pipeline_steps/aio_prepare.groovy'
        deploy = load 'pipeline_steps/deploy.groovy'
        tempest = load 'pipeline_steps/tempest.groovy'
        holland = load 'pipeline_steps/holland.groovy'
        maas = load 'pipeline_steps/maas.groovy'
        kibana = load 'pipeline_steps/kibana.groovy'
    }
    // We need to checkout the rpc-openstack repo on the CIT Slave
    // so that we can cater for builds on the more restricted images
    // used for artifact-based builds.
    if (env.STAGES.contains("Upgrade")) {
      common.prepareRpcGit(env.UPGRADE_FROM_REF, env.WORKSPACE)
    } else {
      common.prepareRpcGit("auto", env.WORKSPACE)
    }
    if(common.is_doc_update_pr("${env.WORKSPACE}/rpc-openstack")){
      return
    }
    pubcloud.runonpubcloud {
      // try within pubcloud node so we can archive archive_artifacts
      // after a failure, before the node is cleaned up.
      try {
        environment_vars = [
          "DEPLOY_HAPROXY=yes",
          "DEPLOY_AIO=no",
          "DEPLOY_MAAS=no",
          "DEPLOY_TEMPEST=no",
          "DEPLOY_SWIFT=${DEPLOY_SWIFT}",
          "DEPLOY_CEPH=${DEPLOY_CEPH}",
          "DEPLOY_ELK=${DEPLOY_ELK}",
          ]
        aio_prepare.prepare()
        deploy.deploy_sh(environment_vars: environment_vars)
        deploy.addChecksumRule()
        maas.deploy()
        maas.verify()
        tempest.tempest()
        kibana.kibana()
        holland.holland()
        if (env.STAGES.contains("Upgrade")) {
          deploy.upgrade(environment_vars: environment_vars)
          deploy.addChecksumRule()
          maas.deploy()
          maas.verify()
          tempest.tempest()
          kibana.kibana()
          holland.holland()
        }
      } catch (e) {
        print(e)
        throw e
      } finally {
        common.archive_artifacts()
      }
    }
  }
}
