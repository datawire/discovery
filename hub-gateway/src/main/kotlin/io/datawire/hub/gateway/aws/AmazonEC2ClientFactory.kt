package io.datawire.hub.gateway.aws

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.AmazonEC2Client


interface AmazonEC2ClientFactory {
  fun newInstance(): AmazonEC2 {
    return AmazonEC2Client(DefaultAWSCredentialsProviderChain())
  }
}
