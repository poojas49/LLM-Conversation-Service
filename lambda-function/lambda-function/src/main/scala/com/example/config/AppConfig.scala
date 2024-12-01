package com.example.config

/**
 * Configuration classes for the application
 *
 * BedrockConfig: Holds AWS Bedrock-specific configuration
 * - region: AWS region where Bedrock is deployed
 * - modelId: Identifier for the specific Bedrock model to use
 *
 * AppConfig: Singleton object providing application-wide configuration
 * - Loads configuration from environment variables with sensible defaults
 * - Centralizes configuration management for easier updates
 */
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