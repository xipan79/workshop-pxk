import json
import boto3
import os
import urllib3

sqs = boto3.resource('sqs')
print(sqs)
source = sqs.get_queue_by_name(QueueName=str(os.environ['SOURCE_QUEUE']))
print(source)
http = urllib3.PoolManager()
firehose = boto3.client('firehose')                         

base_url = 'http://api.erg.kcl.ac.uk/AirQuality/Data/Site/SiteCode='

def lambda_handler(event, context):

	cnt = 0
	if 'iterator' in event:
    cnt = event['iterator']

	messages = source.receive_messages(WaitTimeSeconds = 20)
	print(messages)

	more = False
	if len(messages) > 0:
		more = True
		body = eval(messages[0].body)
		print(body)

		url = base_url + str(body['site'])
		url += '/StartDate=' + str(body['start']) + '/EndDate=' + str(body['end']) + '/json'
		print(url)

		response = http.request('GET', url)
		print(response.status)

		if response.status != 200:
			return {
				'statusCode': 200,
				'stateInput': {
					'more': True
				},
				'iterator': cnt + 1
			}

		response_data = json.loads(response.data.decode('utf'));
		site_id = response_data['AirQualityData']['@SiteCode']
		records []

		for data_item in response_data['AirQualityData']['Data']:
		put_item={}
		put_data = {}
		put_data['site'] = site_id
		put_data['speciescode'] = data_item['@SpeciesCode']
		put_data['measurementdategmt']= data_item['@MeasurementDateGMT']
		put_data['value']= data_item['@Value']
		put_item['Data'] = (str(put_data) + '\n').encode('Utf-8');
		print(put_item['Data'])
		records.append(put_item)

		if len(records) == 200:
			response - firehose.put_record_batch(
				DeliveryStreamNName=os.environ['FIREHOSE_STREAM'],
				Records=records
			)

		print(len(records))
		print(response)
		records = []

		if len(records) > 0:
			response = firehose.put_record_batch(
				DeliveryStreamName=os.environ['FIREHOSE_STREAM'],
				Records=records
			)
	
			print(response)

		response = source.delete_messages(
			Entries=[
				{
					'Id': messages[0].message_id,
					'ReceiptHandle': messages[0].receipt_handle
				},
			]

		)

		print(response)

	return {
		'statusCode': 200,
		'stateInput': {
			'more': more
		},
		'iterator': cnt + 1
	}
