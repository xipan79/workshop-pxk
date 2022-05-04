import boto3
import pandas as pd

# initialize client and start execution
athena_client = boto3.client ('athena')
execution_id = athena_client.start_query_execution(
	QueryString='SELECT distinct measurementdategmt, value FROM 'aaa'.'step_function2021' where site=\'HF4\''
				+ 'and speciescode=\'NO2\' order by measurementdategnt ASC',
	ClientRequestToken='client-request-sample-token-xxxx',
	QueryExecutionContext={
		'Database': 'aaa',
		'Catalog': 'AwsDataCatalog'
	},
	ResultConfiguration={
		'OutputLocation' : 's3://aq-test-tokyo/source/'
	})

# wait until execution is complete
execution_state = athena_client.get_query_execution(QueryExecutionId = execution_id['QueryExecutionId'])
output_location = execution_state['QueryExecution']['ResultConfiguration']['OutputLocation']
print(output_location)

while execution_state['QueryExecution']['Status']['State'] in ('QUEUED','RUNNING'):
	execution_state = athena_client.get_query_execution (QueryExecutionId = execution_id['QueryExecutionId'])

# print execution result
print(execution_state['QueryExecution']['Status']['State'])

# print error messages if result is failed
if 'FAILED' == execution_state['QueryExecution']['Status']['State']:
	print(execution_state)

# plot data
original_data = pd.read_esv(output_location, sep=",", index_col=
							0, parse_dates=True, decimal=".")
original_data.plot()
