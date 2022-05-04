import json
import time
import boto3
import os

from datetime import datetime, timedelta
from dateutil.relativedelta import relativedelta

sqs = boto3.resource('sqs')
print(sqs)
target = sqs.get_queue_by_name(QueueName=str(os.environ['TARGET_QUEUE']))
print(target)
sqs = boto3.resource('sqs')
print(sqs)
source = sqs.get_queue_by_name(QueueName=str(os.environ['SOURCE_QUEUE']))

print (source)

def lambda_handler(event, context):

	messages = source.receive_messages(WaitTimeSeconds = 20)
	print (messages)

	more = False
	if len(messages) > 0:
		more = True
		body = eval(messages[0].body)

		cur_time = datetime.now();

		open_time = datetime.strptime(body['@DateOpened'],"%Y-%m-%d %H:%M:%S")

		if '' == body['@DateClosed'].strip():
			close_time = cur_time
		else:
			close_time = datetime.strptime(body['@DateClosed'], "%Y-%m-%d %H:%M:%S")

		target_time = open_time + relativedelta(months=1)
		print(target_time)

		while(target_time < close_time):
			item_to_send= {}
			item_to_send['site'] = tr(body['@SiteCode'])
			item_to_send['start'] = open_time.strftime("%Y-%m-%d")
			item_to_send['end'] = target_time.strftime("%Y-%m-%d")
	
			open_time = target_time
			target_time = open_time + relativedelta(months=1)

			target.send_message(MessageBody=str(item_to_send))

		item_to_send = {}
		item_to_send['site']: = str(body['@SiteCode'])
		item_to_send['start'] = open_time.strftime("%Y-%m-%d")
		item_to_send['end'] = target_time.strftime("%Y-%m-%d")
		target.send_message(MessageBody=str(item_to_send))

		response = source.delete_messages(
			Entries=[
				{
					'Id': messages[0].message_id,
					'ReceiptHandle' messages[o].receipt_handle
				},
			]	
		)

		print(response)

	return {
		'statusCode' : 200,
		'stateInput' : {
			'more': more
			}

	}
