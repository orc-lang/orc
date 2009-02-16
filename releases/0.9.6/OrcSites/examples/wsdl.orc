include "net.inc"
val amortization = Webservice("http://www.kylehayes.info/webservices/AmortizationCalculator.cfc?wsdl")
{- Parameters: principal, interest rate, number of payments -}
amortization.calculate(100, 0.2, 10)