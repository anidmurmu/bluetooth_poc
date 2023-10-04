import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'dart:async';

import 'package:bluetooth_connector/bluetooth_connector.dart';
import 'package:permission_handler/permission_handler.dart';

class BluetoothApp extends StatefulWidget {
  @override
  _BluetoothState createState() => _BluetoothState();
}

class _BluetoothState extends State<BluetoothApp> {
  BluetoothConnector flutterbluetoothadapter = BluetoothConnector();
  StreamSubscription? _btConnectionStatusListener, _btReceivedMessageListener;
  String _connectionStatus = "NONE";
  List<BtDevice> devices = [];
  String? _recievedMessage;
  TextEditingController _controller = TextEditingController();

  @override
  void initState() {
    super.initState();
    flutterbluetoothadapter
        .initBlutoothConnection("20585adb-d260-445e-934b-032a2c8b2e14");
    flutterbluetoothadapter
        .checkBluetooth()
        .then((value) => print(value.toString()));
    _startListening();
  }

  _startListening() {
    _btConnectionStatusListener =
        flutterbluetoothadapter.connectionStatus().listen((dynamic status) {
      setState(() {
        _connectionStatus = status.toString();
      });
    });
    _btReceivedMessageListener =
        flutterbluetoothadapter.receiveMessages().listen((dynamic newMessage) {
      setState(() {
        _recievedMessage = newMessage.toString();
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Plugin example app'),
      ),
      body: Column(
        children: <Widget>[
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: <Widget>[
              Flexible(
                fit: FlexFit.tight,
                child: Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: OutlinedButton(
                    onPressed: () async {
                      checkPermission(context);
                      //var list = await flutterbluetoothadapter.getDevices();
                      //print("apple +  : $list");
                      var isTrue = true;
                      //isTrue = !isTrue;
                      if (isTrue) {
                        //checkPermission(context);
                        //final isDevice = await flutterbluetoothadapter.startScanBtDevices();
                      }
                    },
                    child: const Text('Toggle Bluetooth'),
                  ),
                ),
              ),
              Flexible(
                fit: FlexFit.tight,
                child: Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: OutlinedButton(
                    onPressed: () async {
                      devices = await flutterbluetoothadapter.getDevices();
                      setState(() {});
                    },
                    child: const Text('LIST DEVICES'),
                  ),
                ),
              )
            ],
          ),
          Text("STATUS - $_connectionStatus"),
          Padding(
            padding: const EdgeInsets.symmetric(
              horizontal: 8.0,
              vertical: 20,
            ),
            child: ListView(
              shrinkWrap: true,
              children: _createDevices(),
            ),
          ),
          Text(
            _recievedMessage ?? "NO MESSAGE",
            style: const TextStyle(fontSize: 24),
          ),
          Row(
            children: <Widget>[
              Flexible(
                flex: 4,
                fit: FlexFit.tight,
                child: Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: TextField(
                    controller: _controller,
                    decoration:
                        const InputDecoration(hintText: "Write message"),
                  ),
                ),
              ),
              Flexible(
                fit: FlexFit.tight,
                child: Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: OutlinedButton(
                    onPressed: () {
                      flutterbluetoothadapter.sendMessage(
                          _controller.text ?? "no msg",
                          sendByteByByte: false);
//                        flutterbluetoothadapter.sendMessage(".",
//                            sendByteByByte: true);
                      _controller.text = "";
                    },
                    child: const Text('SEND'),
                  ),
                ),
              )
            ],
          )
        ],
      ),
    );
  }

  _createDevices() {
    if (devices.isEmpty) {
      return [
        const Center(
          child: Text("No Paired Devices listed..."),
        )
      ];
    }
    List<Widget> deviceList = [];
    devices.forEach((element) {
      deviceList.add(
        InkWell(
          key: UniqueKey(),
          onTap: () {
            flutterbluetoothadapter.startClient(devices.indexOf(element), true);
          },
          child: Container(
            padding: const EdgeInsets.all(4),
            decoration: BoxDecoration(border: Border.all()),
            child: Text(
              element.name.toString(),
              style: const TextStyle(fontSize: 18),
            ),
          ),
        ),
      );
    });
    return deviceList;
  }

  showAlertDialog(context) => showCupertinoDialog<void>(
      context: context,
      builder: (BuildContext context) => CupertinoAlertDialog(
            title: const Text("Permission Denied"),
            content: const Text("Allow access to Bluetooth permission"),
            actions: <CupertinoDialogAction>[
              CupertinoDialogAction(
                  isDestructiveAction: true,
                  child: const Text("Cancel"),
                  onPressed: () {
                    Navigator.pop(context);
                  }),
              CupertinoDialogAction(
                  isDefaultAction: true,
                  onPressed: () => {openAppSettings()},
                  child: const Text("Go to Settings"))
            ],
          ));

  checkPermission(BuildContext context) async {
    final requestConnectStatus = await Permission.bluetoothConnect.request();
    final requestScanStatus = await Permission.bluetoothScan.request();
    final requestBluetoothStatus = await Permission.bluetooth.request();
    //final requestBluetoothAdminStatus = await Permission.bluetooth.request();
    final requestLocationStatus = await Permission.location.request();
    if (requestConnectStatus.isGranted &&
        requestScanStatus.isGranted &&
        requestBluetoothStatus.isGranted &&
        requestLocationStatus.isGranted) {
      //flutterbluetoothadapter.toggleBlutoothConnection();
      flutterbluetoothadapter.startScanBtDevices();
    } else {
      showAlertDialog(context);
    }
  }
}
