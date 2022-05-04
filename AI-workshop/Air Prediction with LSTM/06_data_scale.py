import pandas as pd  
import boto3  
import numpy as np  
 
from pandas import DataFrame  
from pandas import concat  
from sklearn.preprocessing import MinMaxScaler  

#序列转换为LSTM结构  

def timeseries_to_supervised(data, lag=1):  
    df = DataFrame(data)  
    columns = [df.shift(i) for i in range(1, lag+1)]  
    columns.append(df)  
    df = concat(columns, axis=1)  
    df.fillna(0, inplace=True)  
    return df  

#数据归一化  
def scale (train, test):  
    # fit scaler  
    scaler = MinMaxScaler(feature_range=(0, 1))  
    scaler = scaler.fit(train)  
    # transform train  
    train = train.reshape (train. shape[0], train.shape[1])  
    train_scaled =scaler.transform(train)  
    # transform test  
    test = test.reshape(test.shape[0], test. shape[1])  
    test_scaled = scaler.transform(test)  
    return scaler, train_scaled, test_scaled  

source_file = "2017-06-01 13:00:00.csv"  
training_scaled = "training_scaled.npy"  

df = pd.read_csv("data-hf4/" + source_file)  
df['measurementdategnt'] = pd.to_datetime(df['measurementdategnt'])  
df = df.sort_values('measurementdategmt')  
series = df['value'][0:2000]  
raw_values = series.values  
supervised = timeseries_to_supervised(raw_values, 3)  
supervised_values = supervised.values  
train, test = supervised_values[0:-1400], supervised_values[-200:]  
scaler, train_scaled, test_scaled = scale(train, test)  
print(train_scaled)  
print()  
print(test_scaled)  
np.savetxt(training_scaled, train_scaled)  
np.savetxt('test.npy', test_scaled)  
s3_client = boto3.client('s3')  
s3_client.upload_file(training_scaled, "sagemaker-ap-northeast-1-808242303800", "sagemaker/lstm/" + training_scaled)  
s3_client.upload_file("test.npy", "sagemaker-ap-northeast-1-808242303800", "sagemaker/lstm/test.npy")  
