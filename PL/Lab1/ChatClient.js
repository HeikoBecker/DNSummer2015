﻿var groupNumber = 25; // replace by your group number

/*
 * Gloabl Variables 
 */
var name = "";
var socket = null;
var userId = 0;
var msgId = 0;
var msgText = "";

function main() {
    document.getElementById("groupid").textContent = "Group " + groupNumber;
    document.getElementById("loginButton").disabled = true;
}

/* 
 * Function to send a single command.
 * First argument must be the 4 letter command.
 * The second argument is the number that is necessary as specified in the protocol
 * The third argument are the optional lines for the message
 */
function sendMessage(command, number, lines) {
    var msg = command + " " + number; //construct message header
    if (lines != null) { //construct body only if there are any lines given
        msg += "\r\n"; //next line in the message
        // iterate over all given lines and construct body by separating each by
        // "\r\n"
        for (var i = 0; i < lines.length; i++) {
            msg += lines[i];
            if (i < lines.length - 1) {
                msg += "\r\n";
            }
        }
    }
    //Send out the message
    socket.send(msg);
}

/*
 * Message parser
 * Takes the raw input and returns an object with the command, the reference
 * number and the lines in the body
 */
function parseMessage(raw) {
    var lines = raw.split("\r\n"); //"\r\n" is the line separator --> use it for splitting
    var firstLine = lines[0].split(" "); // the number and command are separated by a space

    return {
        command: firstLine[0],
        reference: parseInt(firstLine[1], 10),
        lines: lines.slice(1)
    };
}

// Called when the "Connect" button is pressed
function connectButtonPressed() {
    var server = document.getElementById("serverInput").value;
    document.getElementById("connect").disabled = true;
    document.getElementById("disconnect").disabled = false;
    setStatusBarText("Connecting to " + server + "...");

    //Establish a WebSocket connection to server
    socket = new WebSocket("ws://" + server);
    //and set the methods used for handling events
    socket.onclose = onClose;
    socket.onerror = onError;
    socket.onopen = function () {
        onConnected(server);
    };
    socket.onmessage = onMessage;
}

/*
 * Message handler function
 * First parses the message from the server.
 * Action must be taken depending on whether client is logged in or not.
 * Forwards each action to a specified handler method.
 */
function onMessage(event) {
    var msg = parseMessage(event.data);

    if (msg.command === "INVD") {
        recoverFromFatalError(); //Recover from internal error
    }
    else if (!isLoggedIn) { // not in authenticated, but in connected state
        switch (msg.command) {
            case "OKAY":
                if (userId == msg.reference) {
                    onLoginSuccess();
                } else {
                    onLoginFailed();
                }
                break;
            case "FAIL":
                handleAuthFail(msg);
                break;
            // default case only syntactically needed by switch case,
            // protocol allows no other commands here
            // ensures that future changes to the protocol will get noticed
            default:
                console.log(" Unhandled Command received from server:");
                console.error(msg);
                break;
        }
    } else { // successfully authenticated before
        switch (msg.command) {
            case "ARRV":
                addUser(msg.reference, msg.lines[0], msg.lines[1]);
                break;
            case "LEFT":
                removeUser(msg.reference);
                break;
            case "SEND":
                messageReceived(msg);
                break;
            case "ACKN":
                markMessageAcknowledged(msg.reference, findUserName(parseInt(msg.lines[0], 10)));
                break;
            case "OKAY":
                if (msgId == msg.reference) {
                    addChatMessage(msgId, name, msgText, true);
                    markMessageConfirmed(msgId);
                    afterSuccessfulSend();
                } else {
                    console.log(" Unhandled case where server response does not contain the number (" + msgId + ") sent previously:");
                    console.error(msg);
                }
                break;
            case "FAIL":
                handleFail(msg);
                break;
            // default case only syntactically needed by switch case,
            // protocol allows no other commands here
            // ensures that future changes to the protocol will get noticed
            default:
                console.log(" Unhandled Command received from server:");
                console.error(msg);
                break;
        }
    }
}

/*
 * Socket error handler function
 * Logs the error and calls function for connection fails
 */
function onError(event) {
    console.error(event);
    onConnectionFailed();
}
/*
 * Connetion Close handler, called when socket is closed.
 * Disables login button and send button, cleares global socket variable
 */
function onClose() {
    document.getElementById("loginButton").disabled = true;
    document.getElementById("sendButton").disabled = true;
    socket = null;
    onDisconnected();
}

// Called when the "Disconnect" button is pressed
function disconnectButtonPressed() {
    document.getElementById("disconnect").disabled = true;
    document.getElementById("login").disabled = true;
    setStatusBarText("Disconnecting...");
    // Close socket on disconnect
    socket.close();
}

// Called when the "Log in" button is pressed
function loginButtonPressed() {
    name = document.getElementById("nameInput").value;
    var password = document.getElementById("passwordInput").value;
    document.getElementById("login").disabled = true;
    setStatusBarText("Authenticating...");
    // Set the global user id variable to a valid random number
    userId = getRandomInt();
    // Send login message
    sendMessage("AUTH", userId, [name, password]);
}

// Called when the "Send" button is pressed
function sendButtonPressed() {
    var message = document.getElementById("messageInput").value;
    if (message == "") return;
    var to = "*";
    if (message.substring(0, 1) == "@") {
        var index = message.indexOf(":");
        if (index > 0) {
            var toUser = findUserNumber(message.substring(1, index));
            if (toUser !== -1) to = toUser;
            else {
                addInfoMessage("Unknown user: " + message.substring(1, index) + ".");
                return;
            }
            message = message.substring(index + 1);
        }
    }
    message = message.trim();
    if (message == "") return;
    setStatusBarText("Sending message...");

    // Send the message as specified in the protocol
    msgId = getRandomInt();
    msgText = message;
    sendMessage("SEND", msgId, [to, message]);
}

/*
 * Function to notify user that message sending was successfull, used by
 * Command handler method onMessage
 */
function afterSuccessfulSend() {
    document.getElementById("messageInput").value = "";
    setStatusBarText("Message sent.");
}

// Use this function to get random integers for use with the Chat protocol
function getRandomInt() {
    return Math.floor(Math.random() * 9007199254740991);
}

/*
 * Message receiver function
 * Displays a message, by extracting the values
 * Sends Acknowledge Command for the message to the server
 */
function messageReceived(msg) {
    var senderId = msg.lines[0];
    var senderName = findUserName(senderId);
    var msgId = msg.reference;
    var txt = "";
    for (var i = 1; i < msg.lines.length; i++) {
        txt = txt + msg.lines[i];
    }
    addChatMessage(msgId, senderName, txt, false);
    // Acknowledge message receipt
    sendMessage("ACKN", msgId, null);
}

/*
 * Authentication Fail handler
 * Prints failures in the status bar based on the server returned failure
 */
function handleAuthFail(msg) {
    switch (msg.lines[0]) {
        case "NAME":
            setStatusBarText("Username already in use. Please use a different one.");
            break;
        case "PASSWORD":
            setStatusBarText("Sorry. Wrong password.");
            break;
        case "NUMBER":
            setStatusBarText("Internal Error. Please retry.");
            break;
        // default case that should not occur and is only inserted for
        // syntactical purposes
        // ensures that future changes to the protocol will get noticed
        default:
            console.error("Unhandled Failure for Authentication");
            console.error(msg);
    }
}

/*
 * Message failure handler
 * Prints failure in the status bar based on the server returned failure
 */
function handleFail(msg) {
    switch (msg.lines[0]) {
        case "NUMBER":
            setStatusBarText("Internal Error. Please retry.");
            break;
        case "LENGTH":
            setStatusBarText("Message is too long. Please consider removing some characters.");
            break;
        // default case that should not occur and is only inserted for
        // syntactical purposes
        // ensures that future changes to the protocol will get noticed
        default:
            console.error("Unhandled Failure");
            console.error(msg);
    }
}

function recoverFromFatalError() {
    setStatusBarText("Internal Error occurred. This means that something went wrong while communicating. Recovering...");
    suppressStatusBarUpdate = true;
    // onDisconnected will be called automatically afterwards
}
// The remaining functions in this file are helper functions to update
// the user interface when certain actions are performed (e.g. a message
// is sent and should be displayed in the message list) or certain
// events occur (e.g. a message arrives, or a user has gone offline).
// You should not need to modify them, but you can if you want.
// You can also just delete everything (including the functions above)
// and write a new user interface on your own.

// Call this function when the connection to the server has been established
function onConnected(server) {
    if (server === undefined) document.getElementById("connectionStatusText").textContent = "Connected.";
    else document.getElementById("connectionStatusText").textContent = "Connected to " + server + ".";
    document.getElementById("connect").style.display = "none";
    document.getElementById("disconnect").style.display = "flex";
    document.getElementById("connect").disabled = false;
    document.getElementById("login").disabled = false;
    document.getElementById("loginButton").disabled = false;
    setStatusBarText("Connected.");
}

var isLoggedIn = false;
var suppressStatusBarUpdate = false;

// Call this function when the connection to the server has been closed
function onDisconnected() {
    document.getElementById("disconnect").style.display = "none";
    document.getElementById("connect").style.display = "flex";
    document.getElementById("connect").disabled = false;
    document.getElementById("disconnect").disabled = false;
    document.getElementById("login").disabled = true;
    document.getElementById("message").setAttribute("disabled", "true");
    document.getElementById("userlist").setAttribute("disabled", "true");
    if (!suppressStatusBarUpdate) setStatusBarText("Disconnected.");
    suppressStatusBarUpdate = false;
    if (isLoggedIn) addInfoMessage("Session ended, no more messages will be received.");
    clearUsers();
    isLoggedIn = false;
    name = "";
    // clear our variables too
    socket = null;
    userId = 0;
    msgId = 0;
}

// Call this function when the connection to the server fails (i.e. you get an error)
function onConnectionFailed() {
    setStatusBarText("Connection failed.");
    suppressStatusBarUpdate = true; // onDisconnected should also get called
}

// Call this function when login was successful
function onLoginSuccess() {
    setStatusBarText("Successfully logged in.");
    document.getElementById("message").removeAttribute("disabled");
    document.getElementById("userlist").removeAttribute("disabled");
    document.getElementById("loginButton").disabled = true;
    document.getElementById("sendButton").disabled = false;
    addInfoMessage("Session started, now receiving messages.");
    isLoggedIn = true;
}

// Call this function when login failed
function onLoginFailed() {
    setStatusBarText("Login failed.");
    document.getElementById("login").disabled = false;
    isLoggedIn = false;
}

// Call this function to add informational text to the message list
function addInfoMessage(text) {
    var msglist = document.getElementById("msglist");
    var infoDiv = document.createElement("div");
    infoDiv.className = "info";
    infoDiv.appendChild(document.createTextNode(text));
    msglist.appendChild(infoDiv);
    msglist.scrollTop = msglist.scrollHeight;
}

// Call this function to add a chat message to the message list.
// If isSent is true, then it is added as a "sent, but not confirmed"
// message; call markMessageConfirmed when the server has acknowledged
// that it received the message.
function addChatMessage(number, from, text, isSent) {
    var msglist = document.getElementById("msglist");
    var msgDiv = document.createElement("div");
    msgDiv.className = isSent ? "sent" : "received";
    msgDiv.id = "msg" + number;
    var fromDiv = document.createElement("div");
    fromDiv.className = "from";
    fromDiv.appendChild(document.createTextNode(from === null ? "Unknown user" : from));
    msgDiv.appendChild(fromDiv);
    var textDiv = document.createElement("div");
    textDiv.className = "message";
    textDiv.appendChild(document.createTextNode(text));
    msgDiv.appendChild(textDiv);
    if (isSent) {
        var readersDiv = document.createElement("div");
        readersDiv.className = "readers";
        readersDiv.id = "msg" + number + "readers";
        msgDiv.appendChild(readersDiv);
        msgDiv.style.opacity = 0.5;
    }
    msglist.appendChild(msgDiv);
    msglist.scrollTop = msglist.scrollHeight;
}

// Call this function to mark a sent message as confirmed
function markMessageConfirmed(number) {
    var msgDiv = document.getElementById("msg" + number);
    if (!msgDiv) return;
    msgDiv.style.opacity = 1.0;
}

// Call this function to indicate that a message has been acknowledged by a certain user
function markMessageAcknowledged(messageNumber, userName) {
    var msgReadersDiv = document.getElementById("msg" + messageNumber + "readers");
    if (!msgReadersDiv) return;
    markMessageConfirmed(messageNumber);
    var readerSpan = document.createElement("span");
    readerSpan.appendChild(document.createTextNode(userName));
    msgReadersDiv.appendChild(readerSpan);
}

// Call this function to change the text in the status bar
function setStatusBarText(text) {
    document.getElementById("statusbar").textContent = text;
}

var users = [];

// Call this function to show a user as online
function addUser(number, name, description) {
    users.push({number: number, name: name, description: description});
    var userlist = document.getElementById("userlist");
    var userSpan = document.createElement("span");
    userSpan.id = "user" + number;
    var userNameSpan = document.createElement("span");
    userNameSpan.className = "user-name";
    userNameSpan.appendChild(document.createTextNode(name));
    var userDescSpan = document.createElement("span");
    userDescSpan.className = "user-desc";
    userDescSpan.appendChild(document.createTextNode(description));
    userSpan.appendChild(userNameSpan);
    userSpan.appendChild(userDescSpan);
    userlist.appendChild(userSpan);
}

// Call this function when a user goes offline
function removeUser(number) {
    var userlist = document.getElementById("userlist");
    for (var i = 0; i < users.length; ++i) {
        if (users[i].number == number) {
            users.splice(i--, 1);
            var userSpan = document.getElementById("user" + number);
            if (userSpan) userlist.removeChild(userSpan);
        }
    }
}

// Call this function to get the number of a user with the given name.
// Returns -1 if there is no user with this name.
function findUserNumber(name) {
    for (var i = 0; i < users.length; ++i) {
        if (users[i].name == name) return users[i].number;
    }
    return -1;
}

// Call this function to get the name of a user with the given number.
// Returns null if there is no user with this number.
function findUserName(number) {
    for (var i = 0; i < users.length; ++i) {
        if (users[i].number == number) return users[i].name;
    }
    return null;
}

// Called by onDisconnected
function clearUsers() {
    var userlist = document.getElementById("userlist");
    for (var i = 0; i < users.length; ++i) {
        var userSpan = document.getElementById("user" + users[i].number);
        if (userSpan) userlist.removeChild(userSpan);
    }
    users = [];
}
