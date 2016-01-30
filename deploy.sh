#!/usr/bin/env bash

set -e
set -u
set -v
set -x
set -o pipefail

# more bash-friendly output for jq
#JQ="jq --raw-output --exit-status"
JQ="jq -r -e"

deploy_image() {

    docker login -u $DOCKER_USERNAME -p $DOCKER_PASS -e $DOCKER_EMAIL
    docker push ultimatixarup/diamondback-etl:$CIRCLE_SHA1

}

# reads $CIRCLE_SHA1, $host_port
# sets $task_def
make_task_def() {

    task_template='[
	{
	    "name": "etl",
	    "image": "ultimatixarup/diamondback-etl:%s",
	    "essential": true,
	    "memory": 2048,
      "portMappings": [
      {
        "hostPort": %s,
        "containerPort": 9000,
        "protocol": "tcp"
      }
      ],
      "environment": [
                {
                    "name": "AWS_ACCESS_KEY_ID",
                    "value": '.$AWS_ACCESS_KEY_ID.'
                },
                {
                    "name": "AWS_SECRET_ACCESS_KEY",
                    "value": '.$AWS_SECRET_ACCESS_KEY.'
                }
            ]
	}
    ]'

    task_def=$(printf "$task_template" $CIRCLE_SHA1 $host_port)

}

# reads $family
# sets $revision
register_definition() {

    if revision=$(aws ecs register-task-definition --container-definitions "$task_def" --family $family | $JQ '.taskDefinition.taskDefinitionArn'); then
        echo "Revision: $revision"
    else
        echo "Failed to register task definition"
        return 1
    fi

}

deploy_cluster() {

    host_port=80
    cluster="default"
    family="diamondback-etl"
    service="diamondback-etl-service"
    num_tasks=2

    make_task_def
    register_definition
    if [[ $(aws ecs update-service --cluster $cluster --service $service --desired-count $num_tasks --task-definition $revision | \
                   $JQ '.service.taskDefinition') != $revision ]]; then
        echo "Error updating service."
        return 1
    fi

    # wait for older revisions to disappear
    # not really necessary, but nice for demos
    for attempt in {1..50}; do
        if stale=$(aws ecs describe-services --cluster $cluster --services $service | \
                       $JQ ".services[0].deployments | .[] | select(.taskDefinition != \"$revision\") | .taskDefinition"); then
            echo "Waiting for stale deployments:"
            echo "$stale"
            sleep 5
        else
            echo "Deployed!"
            return 0
        fi
    done
    echo "Service update took too long."
    return 1
}

deploy_image
deploy_cluster
