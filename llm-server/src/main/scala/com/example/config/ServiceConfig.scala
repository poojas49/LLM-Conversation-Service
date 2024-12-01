package com.example.config

case class ServiceConfig(
                          apiGatewayUrl: String = sys.env.getOrElse(
                            "API_GATEWAY_URL",
                            "https://scpgq9wnh1.execute-api.us-east-1.amazonaws.com/prod/generate"
                          )
                        )