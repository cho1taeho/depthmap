import 'package:flutter/services.dart';

// 네이티브 연결 채널
class DepthApi {
  static const _channel = MethodChannel('com.example.depth');

  static Future<void> initSession() async {
    await _channel.invokeMethod('initSession');
  }

  static Future<Map<String, dynamic>> capturePhotoWithDepth() async {
    final result = await _channel.invokeMethod('capturePhotoWithDepth');
    return Map<String, dynamic>.from(result);
  }
}