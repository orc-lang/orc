-- Give one site a higher priority than another using a delay

x <x< Adrian("High priority response: ")
      | (Rtimer(10000) >> u)
          <u< David("Low priority response: ")
