import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo Call Native',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: const MyHomePage(title: 'Flutter Demo Call Native'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({Key? key, required this.title}) : super(key: key);
  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  TextEditingController textEditingController = TextEditingController();
  static const platform = const MethodChannel('flutterplugins.javatpoint.com/browser');
  static const platform_login_facebook =
      const MethodChannel('call_native.example.com/facebook_login');
  String tokenFaceBook = '';
  String newNotification = '';
  String flutterSend = '';
  String nativeSendBack = '';

  static const platform_envent_method =
      const EventChannel('call_native.example.com/listen_message');

  static const basic_message_channel =
      BasicMessageChannel('call_native.example.com/listen_basic_message', StringCodec());

  @override
  void initState() {
    eventChannel();
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
      ),
      body: Padding(
        padding: const EdgeInsets.all(8.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Event channel get data from firebase: ' + newNotification),
            Center(
              child: Material(
                color: Colors.blue,
                child: InkWell(
                  onTap: () => openFacebookLogin(),
                  child: const Padding(
                    padding: EdgeInsets.symmetric(vertical: 8, horizontal: 16),
                    child: Text(
                      'Đăng nhập facebook (Method chanel)',
                      style: TextStyle(color: Colors.white),
                    ),
                  ),
                ),
              ),
            ),
            Text(tokenFaceBook),
            const SizedBox(height: 8),
            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: textEditingController,
                    decoration: const InputDecoration(
                      hintText: 'Basic message channel'
                    ),
                  ),
                ),
                Material(
                  color: Colors.blue,
                  child: InkWell(
                    onTap: () =>  onMessageChanel(),
                    child: const Padding(
                        padding: EdgeInsets.symmetric(vertical: 8, horizontal: 16),
                        child: Icon(Icons.send)),
                  ),
                ),
              ],
            ),  const SizedBox(height: 8),
            Text('Flutter send: ' +  flutterSend),
            const SizedBox(height: 8),
            Text('Native send: ' + nativeSendBack),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => openUrl(),
        tooltip: 'Increment',
        child: const Icon(Icons.add),
      ), // This trailing comma makes auto-formatting nicer for build methods.
    );
  }

  openUrl() async {
    try {
      final int result = await platform
          .invokeMethod('call_native_open_url', <String, String>{'url': "https://www.cafedev.vn"});
    } on PlatformException catch (e) {
      // Unable to open the browser
      print(e);
    }
  }

  openFacebookLogin() async {
    try {
      final String result = await platform_login_facebook.invokeMethod('open_facebook');
      setState(() {
        tokenFaceBook = result;
      });
      print('Flutter call back: ' + result);
    } on PlatformException catch (e) {
      // Unable to open the browser
      print(e);
    }
  }

  openEventMethod() {}

  onMessageChanel() {

    basic_message_channel.send(textEditingController.text);
    basic_message_channel.setMessageHandler((message) async {
      print('received message: ' + message.toString());
      setState(() {
        flutterSend =  textEditingController.text;
        nativeSendBack = message.toString();
      });
      return 'Send message back';
    });

  }

  void eventChannel () async{
    FlutterLocalNotificationsPlugin flutterLocalNotificationsPlugin =
    FlutterLocalNotificationsPlugin();
    AndroidInitializationSettings initializationSettingsAndroid =
    const AndroidInitializationSettings('mipmap/ic_launcher');
    final InitializationSettings initializationSettings = InitializationSettings(
      android: initializationSettingsAndroid,
    );
    await flutterLocalNotificationsPlugin.initialize(initializationSettings,
        onSelectNotification: (s){});
    const AndroidNotificationDetails androidPlatformChannelSpecifics =
    AndroidNotificationDetails('your channel id', 'your channel name',
        channelDescription: 'your channel description',
        importance: Importance.max,
        priority: Priority.high,
        ticker: 'ticker');
    const NotificationDetails platformChannelSpecifics =
    NotificationDetails(android: androidPlatformChannelSpecifics);
    platform_envent_method.receiveBroadcastStream().listen((event) async{

      await flutterLocalNotificationsPlugin.show(
          0, 'Notification', event, platformChannelSpecifics,
          payload: 'item x');
      print(event);
      setState(() {
        newNotification = event;
      });
    }).onError((error) => throw 'Platform exception');
  }
}
