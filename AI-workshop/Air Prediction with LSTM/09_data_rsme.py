
from math import sqrt
from sklearn.metrics import mean_squared_error
from matplotlib import pyplot
rmse = sqrt(mean_squared_error(raw_values[-200:], predictions))

print('Test RMSE: %.3f' % rmse)
# line plot of observed vs predicted
pyplot.plot(raw_values[-200:], '-r', label = 'Test data', linewidth = 2)
pyplot.plot(predictions, '-k', label = 'LSTM predicitons', linewidth = 2)
pyplot.legend()
pyplot.xlabel ('Time count', fontweight = 'bold')
pyplot.ylabel('Pollutant concentration', fontweight = 'bold')
pyplot.show()
