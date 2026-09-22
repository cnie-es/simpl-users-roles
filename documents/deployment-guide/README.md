# Deployment Guide

This guide provides instructions for deploying the Onboarding service using Helm charts in a Kubernetes environment.

## Prerequisites

- A Kubernetes cluster
- Helm - the package manager for Kubernetes. See https://helm.sh/
- kubectl - the Kubernetes command-line tool that allows you to run commands against Kubernetes clusters. See https://kubernetes.io/docs/reference/kubectl/kubectl/

> **⚠️** *Step only needed for the governance authority*

## Deployment Steps

```shell
# 771 is the project_id of this repository
helm repo add users-roles-charts https://code.europa.eu/api/v4/projects/771/packages/helm/stable

helm install users-roles users-roles-charts/users-roles \
--version <chart version> \ 
--values values.yaml
```

> **💡** The latest `2.7.x` chart version can be found in the releases section of the project's repository. Please note that hotfixes may have been released since the last major update. Hotfixes may have been released. [Link to releases](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/releases).

#### Kafka Configuration
To configure kafka, you need to update your values.yaml with the entry `kafkaConfig`.

- The value `kafkaConfig.spring.kafka` can be built following the spring documentation for the standard kafka configuration
- The value `kafkaConfig.simpl.kafka.topic.prefix` is used to differentiate the topics for the kafka inbox/outbox pattern. Don't change it.


#### Example of values.yaml
```yaml
hostT1: < participant or authority backend > # example: t1.authority.dev.simpl-europe.eu or t1.participant1.dev.simpl-europe.eu
authorityHostT2: < authority tier-2 url >  # example: t2.authority.dev.simpl-europe.eu

db:
  url: "jdbc:postgresql://postgresql.{{ .Release.Namespace }}.svc.cluster.local:5432/usersroles"
  username: "usersroles"
  password: "usersroles"

redis:
  host: "redis-master.{{ .Release.Namespace }}.svc.cluster.local"
  port: "6379"
  username: "default"
  password: "admin" # update this

keycloak:
  master:
    user: "user"
    password: "admin"
    realm: <authority or participant>
  clientToRealmRoleMigration:
    enabled: true # set to true to import realm roles from client roles
    clientIds: "['frontend-cli']" # Add clients from which you want to copy the roles, separated by commas (e.g., ['frontend-cli','client-1','client-2']).
    
microservices:
  authenticationProviderUrl: http://authentication-provider.{{ .Release.Namespace }}.svc.cluster.local:8080
# uncomment if you are deploying an authority

databaseSeeding:
  roleIdentityAttributesMapping:
    # If you want to start with an empty identity attributes table, set this to false.
    # if enabled is set to true and filePath is commented or empty the table will be initialized with the default seeding file.
    enabled: true 

    # To specify your custom attributes, uncomment and provide the path to your JSON file here.
    # filePath must start with 'file:'
    # You can find an example of seeding file in repo Charts folder samples/db-seeding/user-roles/0.8.x/roleAttributes.default.json
    # filePath: file:/app/config/exampleRoleAttributes.json
        
  role-persistence-migration:
    # To configure roles to exclude from keycloak to persistence roles migration
    excludeRoles: "default-roles-participant,offline_access,uma_authorization"

kafkaConfig:
  spring:
    kafka:
      bootstrap-servers: <your-kafka-url>
      consumer:
        group-id: users-roles
        properties:
          security.protocol: SASL_PLAINTEXT
          sasl.mechanism: PLAIN
          sasl.jaas.config: org.apache.kafka.common.security.plain.PlainLoginModule required username="${KAFKA_USER:<your-kafka-username>}" password="${KAFKA_PASSWORD:your-kafka-password}";
      admin:
        auto-create: true

  simpl:
    kafka:
      topic:
        prefix: iaa.{{ .Release.Namespace }}.
      inbox:
        topic:
          pattern: iaa\.{{ .Release.Namespace }}\..*

```

## Additional Documentation

For more information, please refer to the [documentation repository](https://code.europa.eu/simpl/simpl-open/development/iaa/documentation). Select the correct version based on the release.

For a full overview of configuration properties, please refer to [Properties file](../installation-guide/PROPERTIES.md).