from cpgqls_client import CPGQLSClient, import_code_query, workspace_query
import re

server_endpoint = "localhost:8080"
client = CPGQLSClient(server_endpoint)

# execute a simple CPGQuery
query = import_code_query("/Users/phli/llm4pa/joern/2.js", "test-app")
result = client.execute(query)
print(result['stdout'])
