import os
import sagemaker

from sagemaker import get_execution_role
from sagemaker.tensorflow import TensorFlow

role = get_execution_role()

training_data_uri = "s3://sagemaker-ap-northeast-1-808242303800/sagemaker/lstm/"

lstm_estimator= TensorFlow(
	entry_point="lstm.py",
	role=role,
	instance_count=1,
	instance_type="ml.m5.xlarge",
	framework_version="2.1",
	py_version="py3",
	distribution={"parameter_server": {"enabled": False}},
)
lstm_estimator.fit(training_data_uri)