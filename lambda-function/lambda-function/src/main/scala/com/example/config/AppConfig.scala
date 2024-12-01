package com.example.config

case class BedrockConfig(
                          region: String,
                          modelId: String
                        )

object AppConfig {
  val bedrockConfig = BedrockConfig(
    region = sys.env.getOrElse("AWS_REGION", "us-east-1"),
    modelId = sys.env.getOrElse("BEDROCK_MODEL_ID", "anthropic.claude-v2")
  )
}