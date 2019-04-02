require(tibble)
require(dplyr)

load_akka_philosopher <- function() {
  akka_philosopher_1 <-
    c(202.566, 161.506, 159.212, 162.035, 203.309, 201.225, 163.749, 201.185, 159.614, 167.311, 171.496, 190.416, 159.447, 170.192, 152.516, 158.524, 160.572, 170.424, 148.562, 161.437)

  akka_philosopher_8 <-
    c(364.835, 337.111, 379.127, 388.848, 352.312, 341.663, 399.802, 398.770, 357.361, 341.572, 399.534, 391.950, 347.182, 350.965, 351.768, 338.351, 332.976, 368.500, 319.148, 339.057)

  akka_philosopher_16 <-
    c( 711.320,  812.850,  694.363,  868.252,  791.189,  705.004,  773.440,  869.800,  659.530,  717.632,  736.057, 1031.793,  708.181,  667.497,  841.069,  688.835,  716.562,  803.877,  704.841,  690.235)

  akka_philosopher_24 <-
    c(1082.393, 1236.650,  977.281,  866.667,  982.213,  975.564,  871.992,  879.383,  889.198,  992.457,  862.795,  903.259,  847.872,  837.718,  931.945,  947.145,  932.344,  950.251,  969.736, 1069.930)

  akka_philosopher_32 <-
    c(1242.941, 1092.692, 1135.124, 1031.970,  944.823, 1129.442, 1064.849, 1190.172, 1074.108, 1065.631, 1221.936, 1137.285, 1270.601, 1272.107, 1048.957, 1064.869, 1147.604,  952.716, 1063.233,  965.970)

  bind_rows(
    tibble(benchmarkName="philosopher (Scala)", benchmarkProblemName="Dining Philosophers", language="Scala", nCPUs=c(1), elapsedTime=akka_philosopher_1 / 1000, rep=seq(10, 29)),
    tibble(benchmarkName="philosopher (Scala)", benchmarkProblemName="Dining Philosophers", language="Scala", nCPUs=c(8), elapsedTime=akka_philosopher_8 / 1000, rep=seq(10, 29)),
    tibble(benchmarkName="philosopher (Scala)", benchmarkProblemName="Dining Philosophers", language="Scala", nCPUs=c(16), elapsedTime=akka_philosopher_16 / 1000, rep=seq(10, 29)),
    tibble(benchmarkName="philosopher (Scala)", benchmarkProblemName="Dining Philosophers", language="Scala", nCPUs=c(24), elapsedTime=akka_philosopher_24 / 1000, rep=seq(10, 29)),
    tibble(benchmarkName="philosopher (Scala)", benchmarkProblemName="Dining Philosophers", language="Scala", nCPUs=c(32), elapsedTime=akka_philosopher_32 / 1000, rep=seq(10, 29))
  )
}

akka_philosopher <- load_akka_philosopher()
