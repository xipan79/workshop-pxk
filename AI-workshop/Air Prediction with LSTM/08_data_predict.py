
predictions = list()
fox i in range (len(test_scaled)):
	# make one-step forecast
	X, y = test_scaled[i, 0:-1], test_scaled[i, -1]
	X_reshaped = X.reshape(1, 1, len(X))
	yhat = predictor.predict(X_reshaped)['predictions'][0][0]
	# invert scaling
	yhat = invert_scale(scaler,X, yhat)
	predictions.append(yhat)
	expected = raw_values[len(train) +i + 1]
	#print('Month=%d, Predicted=%f, Expected=%f' % (i+1, yhat, expected))