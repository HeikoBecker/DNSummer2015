#!/usr/bin/python

import sys
import os
import string

MAX=int(sys.argv[1])
RUNS=sys.argv[2]

totalTimes = {}
timeouts = {}
retransmissions = {}

os.mkdir("tmp")

for i in range(1,MAX+1):
    timeout = i * 5 + 10
    timeouts[i] = timeout
    print("Run number "+str(i)+" with timeout: " + str(timeout))
    
    filename="tmp/modes_run_"+str(i)+".txt"
    cmd = "modes.exe go-back-n.modest -E \"N=9,TIMEOUT="+str(timeout)+"\" --resolve-uniformly '{rdt_snd,rdt_rcv_s}' -N "+RUNS+" > "+filename
    print cmd
    os.system(cmd)

    print ("Parsing") 

    result = open(filename, 'r')
    TotalTime = False
    Retransmissions = False
    for line in result:
        if TotalTime:
            if "Mean" in line:
                values = line.split(":")
                print ("TotalTime for run " +str(i)+":")
                print (values[1])
                TotalTime = False
                totalTimes[i] = float(values[1])
        if Retransmissions:
            if "Mean" in line:
                values = line.split(":")
                print ("Retransmissions for run " +str(i)+":")
                print (values[1])
                Retransmissions = False
                retransmissions[i] = float(values[1])
        if "Property TotalTime" in line:
            TotalTime = True
        if "Property Retransmissions" in line:
            Retransmissions = True

runTimes = 1
minimumTime = totalTimes[runTimes]
runRet = 1
minimumRetrans = retransmissions[runRet]

print ("Run Timeout\t|| TotalTime \t\t|| Retransmissions")
for i in totalTimes.keys():
    if (minimumTime > totalTimes[i]):
        minimumTime = totalTimes[i]
        runTimes = i
    if (minimumRetrans > retransmissions[i]):
        minimumRetrans = retransmissions[i]
        runRet=i
    timeStr = str(totalTimes[i]).replace("\n","") + "\t"
    retransStr = str(retransmissions[i]).replace("\n","")
    print (str(timeouts[i])+"\t||"+ timeStr+"||"+retransStr)

print ("For N="+str(runTimes)+ " minimal time can be achieved as "+ str(minimumTime))
print ("For N="+str(runRet) + " minimal retransmissions can be achieved as "+str(minimumRetrans))
