import socket

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect(("localhost", 4711))
s.sendall("HELLO WORLD\n");
print "SENT"
data = s.recv(1024);
s.close()
print "Received", repr(data)