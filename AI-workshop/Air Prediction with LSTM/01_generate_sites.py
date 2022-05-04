import json
import boto3
import os
import urllib3

format = str(os.environ['TARGET_FORMAT'])
sqs = boto3.resource('sqs')
queue = sqs.get_queue_by_name(QueueName=str(os.environ['TARGET_QUEUE']))
http = irllib3.PoolManager()

def lambda_handler(event, context):

	url = 'http://api.erg.kcl.ac.uk/AirQuality/Information/MonitoringSites/GroupName=London/'

	response = http.request('GET', url)

	r = json.loads(response.data)

	for site in r['Sites']['Site']:                                                                                                                                               
		queue.send_message(MessageBody=str(site))

	return {
		'statusCode': 200,

	}
