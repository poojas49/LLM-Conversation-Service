# LLM-Conversation-Service
A scalable, distributed system for LLM inference and conversation management using cloud (Bedrock) and local models(Ollama).

## System Overview
This project implements a distributed LLM processing system with three main components:
1. **LLM REST Service**: Primary interface for LLM inference (uses GRPC and Protobuf to request data from API-Gateway which then contacts Lambda function deployed on AWS Lambda)
2. **Conversation Client**: Orchestrates conversations between cloud LLM and local Ollama Model. The conversation continues till the terminating condition of max no. of turns is completed.
3. **Lambda Bedrock Integration**: Deserializes/Serializes the base 64 encoded protobuf requests/responses and acts as a Serverless interface to Amazon Bedrock.

## Architecture

### Component Interaction
```mermaid
graph TD
    A[Conversation Client] -->|REST API| B[LLM REST Service]
    A -->|Local Inference| C[Ollama]
    B -->|API Gateway| D[Lambda Function]
    D -->|Model Inference| E[Amazon Bedrock]
    
    style A fill:#f9f,stroke:#333,stroke-width:2px
    style B fill:#bbf,stroke:#333,stroke-width:2px
    style D fill:#bfb,stroke:#333,stroke-width:2px
    style E fill:#ddd,stroke:#333,stroke-width:2px
```

### Data Flow
```mermaid
sequenceDiagram
    participant CC as Conversation Client
    participant RS as REST Service
    participant LF as Lambda Function
    participant BR as Bedrock
    participant OL as Ollama

    CC->>RS: Send initial query
    RS->>LF: Forward request (Protobuf)
    LF->>BR: Invoke model
    BR->>LF: Return response
    LF->>RS: Return result (Protobuf)
    RS->>CC: Return response
    CC->>OL: Generate follow-up
    OL->>CC: Return response
    Note over CC: Continue conversation cycle
```

## Components

### 1. LLM REST Service
The entry point for LLM inference requests.

Key Features:
- RESTful API interface
- Protocol Buffer serialization
- Request/response validation
- Comprehensive logging
- Health monitoring

Configuration (`application.conf`):
```hocon
http {
  host = "0.0.0.0"
  port = 8080
}

service {
  api-gateway-url = "https://your-api-gateway-url"
}
```

### 2. Conversation Client
Orchestrates conversations between cloud and local LLM services.

Key Features:
- Asynchronous processing
- CSV export
- Conversation management
- Performance monitoring
- Local model integration

Configuration (`application.conf`):
```hocon
ollama {
  host = "http://localhost:11434"
  model = "llama2:latest"
}

conversation {
  max-turns = 5
  timeout-minutes = 30
}
```

### 3. Lambda Bedrock Integration
Serverless interface to Amazon Bedrock.

Key Features:
- AWS Lambda integration
- Bedrock model invocation
- Protocol Buffer handling
- Error recovery
- CloudWatch monitoring

Configuration (Environment Variables):
```bash
AWS_REGION=us-east-1
BEDROCK_MODEL_ID=anthropic.claude-v2
```

## Setup

### Prerequisites
- Java 11
- Scala 2.13.10
- SBT 1.x
- AWS Account
- Ollama installed
- Protocol Buffer compiler

### Installation

1. Clone repositories:
```bash
git clone [main-repo-url]
cd llm-server
```

2. Install dependencies:
```bash
# For MacOS
brew install protobuf
brew install scala
brew install sbt
brew install ollama

# For Ubuntu
sudo apt-get update
sudo apt-get install protobuf-compiler scala sbt
# Install Ollama for Linux
```

3. Build all components:
```bash
# Build REST Service
cd llm-server
sbt clean compile assembly

# Build Conversation Client
cd ../conversation-client
sbt clean compile assembly

# Build Lambda Function
cd ../lambda-function
sbt clean compile assembly
```

## Deployment

### 1. Deploy Lambda Function
```bash
aws lambda create-function \
  --function-name bedrock-inference \
  --runtime java11 \
  --handler com.example.LambdaHandler \
  --memory-size 512 \
  --timeout 30 \
  --role [IAM-ROLE-ARN] \
  --zip-file fileb://lambda-function/target/scala-2.13/lambda-function-assembly-0.1.0-SNAPSHOT.jar
```

### 2. Start REST Service
```bash
java -jar llm-rest-service/target/scala-2.13/llm-rest-service-assembly-1.0.jar
```

### 3. Start Conversation Client
```bash
java -jar conversation-client/target/scala-2.13/conversation-client-assembly-1.0.jar
```

## Usage

### Start a Conversation
```bash
curl -X POST http://localhost:8081/conversation \
  -H "Content-Type: application/json" \
  -d '{
    "initialQuery": "Tell me about machine learning",
    "outputFile": "conversation.csv"
  }'
```

### Monitor the Process
1. Check REST Service logs: `logs/application.log`
2. Check Conversation Client logs: `logs/conversation-server.log`
3. Monitor Lambda through CloudWatch

## Error Handling
The system implements comprehensive error handling:

1. **Network Failures**
   - Automatic retries
   - Circuit breaking
   - Timeout handling

2. **Service Errors**
   - Invalid requests
   - Model failures
   - Resource constraints

3. **Integration Issues**
   - Serialization errors
   - Protocol mismatches
   - Configuration problems

## Monitoring

### Metrics
- Request latency
- Processing time per turn
- Memory usage
- Error rates
- Model performance

### Logging
- Structured logging across components
- CloudWatch integration
- Performance tracking
- Error tracing

## Development

### Project Structure
```
llm-conversation-service/
├── llm-server/
│   └── src/
│       └── main/
│           ├── scala/
│           └── resources/
├── conversation-client/
│   └── src/
│       └── main/
│           ├── scala/
│           └── resources/
└── lambda-function/
    └── src/
        └── main/
            ├── scala/
            └── resources/
```

## Troubleshooting

Common issues and solutions:

1. **Connection Errors**
   - Verify service URLs
   - Check AWS credentials
   - Confirm network access

2. **Performance Issues**
   - Monitor memory usage
   - Check log files
   - Verify configuration

3. **Integration Problems**
   - Validate Protocol Buffer versions
   - Check service compatibility
   - Verify AWS permissions
