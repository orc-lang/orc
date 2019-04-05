include "benchmark.inc"

-- Lines: 18
def Arbitrator(n) = 
    val forks = Array(n) >a> (upto(n) >i> a(i) := false >> stop ; a)
    val chan = Channel()
    chan | (
        repeat({ 
            val m = chan.get() 
            m >("request", i, ret)> (
            if forks(i)? || forks((i+1) % n)? then
                ret := false
            else
                (
                forks(i) := true,
                forks((i+1) % n) := true) >>
                ret := true
                ) |
            m >("return", i)> (
                (
                forks(i) := false,
                forks((i+1) % n) := false)
                )
        })
    ) >> stop

-- Lines: 7
def Philosopher(i, 0, arbitrator) = stop
def Philosopher(i, m, arbitrator) =
    val ret = Cell()
    arbitrator.put(("request", i, ret)) >> (
    if ret? then
--        Println("Eating " + (i, m)) >>
        arbitrator.put(("return", i)) >>
        Philosopher(i, m-1, arbitrator)
    else
        Philosopher(i, m, arbitrator)
    )
        
    
    
val N = 20
val M = 10000

-- Lines: 4
def philosophers() =
    val arbitrator = Arbitrator(N)
    upto(N) >i> Philosopher(i, M, arbitrator) ;
    arbitrator.close()

benchmarkSized("philosopher", N * M, { signal }, { _ >> philosophers() }, { _ >> true })
