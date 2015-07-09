#!/usr/bin/python

import sys
import os
import string

MAX=int(sys.argv[1])
RUNS=sys.argv[2]

totalTimes = {}
retransmissions = {}

try:
    os.mkdir("tmp")
except FileExistsError as exc: # Python >2.5
    pass

for i in range(1,MAX+1):
    print("Run number "+str(i))
    filename="tmp/modes_run_"+str(i)+".txt"
    os.system("modes.exe go-back-n.modest -E \"N="+str(i)+"\" --resolve-uniformly '{rdt_snd,rdt_rcv_s}' -N "+RUNS+" > "+filename)

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

print ("Run Nr\t|| TotalTime \t\t|| Retransmissions")
for i in totalTimes.keys():
    if (minimumTime > totalTimes[i]):
        minimumTime = totalTimes[i]
        runTimes = i
    if (minimumRetrans > retransmissions[i]):
        minimumRetrans = retransmissions[i]
        runRet=i
    timeStr = str(totalTimes[i]).replace("\n","") + "\t"
    retransStr = str(retransmissions[i]).replace("\n","")
    print (str(i)+"\t||"+ timeStr+"||"+retransStr)

print ("For N="+str(runTimes)+ " minimal time can be achieved as "+ str(minimumTime))
print ("For N="+str(runRet) + " minimal retransmissions can be achieved as "+str(minimumRetrans))
