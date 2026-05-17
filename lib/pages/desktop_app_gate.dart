import 'package:flutter/material.dart';
import 'package:fluent_ui/fluent_ui.dart' as fluent;
import '../services/audio_source_service.dart';
import '../services/auth_service.dart';
import '../services/persistent_storage_service.dart';
import '../layouts/fluent_main_layout.dart';
import 'desktop_setup_page.dart';

/// 桌面端应用入口控制器
/// 
/// 根据音源配置、协议确认状态和本地模式决定显示引导页还是主布局。
/// 使用内部状态管理避免重建 Navigator。
class DesktopAppGate extends StatefulWidget {
  const DesktopAppGate({super.key});

  @override
  State<DesktopAppGate> createState() => _DesktopAppGateState();
}

class _DesktopAppGateState extends State<DesktopAppGate> {
  @override
  void initState() {
    super.initState();
    AudioSourceService().addListener(_onStateChanged);
    AuthService().addListener(_onStateChanged);
  }

  @override
  void dispose() {
    AudioSourceService().removeListener(_onStateChanged);
    AuthService().removeListener(_onStateChanged);
    super.dispose();
  }

  void _onStateChanged() {
    if (mounted) {
      setState(() {});
    }
  }

  @override
  Widget build(BuildContext context) {
    final isConfigured = AudioSourceService().isConfigured;
    final isTermsAccepted = PersistentStorageService().getBool('terms_accepted') ?? false;
    final isLocalMode = PersistentStorageService().enableLocalMode;

    // 完成协议确认并配置音源后即可进入主布局；本地模式同样只要求确认协议
    if ((isConfigured && isTermsAccepted) || (isLocalMode && isTermsAccepted)) {
      return const FluentMainLayout();
    }

    // 否则显示引导页
    return const DesktopSetupPage();
  }
}
