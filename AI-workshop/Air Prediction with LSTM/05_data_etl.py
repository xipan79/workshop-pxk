import numpy as np
hours_can_fill = 4
def write_to_file(file_start, fragment):
	with open('data-hf4/' + str(file_start) + '.csv', "wb") as fp:
	fp.write(("\"measurementdategmt\",\"value\",\"filled\"\n").encode('utf-8'))
	for d in fragment:
    	str_to_write = "\"" + str(d['time']) + "\",\"" +str(d['value']) + "\",\"" + (str(True) if 'fill' in d else "") + "\"\n"
		fp.write( str_to_write.encode("utf-8"))
	print('data-hf4/' + str(file_start) + '.csv')

last_data = None
last_value = None
file_start = None
fragment = []
for i, line in original_data.iterrows():
	if not np.math.isnan(line['value']):
		if not file_start:
			file_start = i

	if last_data:
		delta = i - last_data
		if delta <= pd.Timedelta(hours=hours_can_fill):
			item_filled =0
			while last_data < i - pd.Timedelta (hours=1):
				item_filled=iten_filled+l
				last_data = last_data + pd.Timedelta(hours=1)
				item = {}
				item['time'] = str(last_data)
				item['value'] = round((last_value + (line['value'] - last_value) (pd. Timedelta(hours=item_filled)/delta)), 1)
				item['fill'] = True
				fragment. append(item)
		else:
			write_to_file(file_start, fragment)
			fragment = []
			file_start = i

	last_data = i
	last_value = line['value']

	item={}
	item['time'] = str(last_data)
	item['value'] = last_value
	fragment. append(item)
	
write_to_file(file_start, fragment)