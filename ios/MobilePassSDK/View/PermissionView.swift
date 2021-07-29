//
//  PermissionView.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 28.07.2021.
//

import SwiftUI

class CurrentPermissionModel: ObservableObject {
    @Published var message:     String  = "text_status_message_need_ble_enabled"
    @Published var showButton:  Bool    = false
    @Published var type:        Int     = NeedPermissionType.NEED_ENABLE_BLE.rawValue
    
    init(type: Int) {
        self.type = type
        
        switch type {
        case NeedPermissionType.NEED_ENABLE_BLE.rawValue:
            self.message = "text_status_message_need_ble_enabled"
            break
        case NeedPermissionType.NEED_ENABLE_LOCATION_SERVICES.rawValue:
            self.message = "text_status_message_need_location_enabled"
            self.showButton = true
            break
        case NeedPermissionType.NEED_PERMISSION_BLUETOOTH.rawValue:
            self.message = "text_status_message_need_permission_bluetooth"
            self.showButton = true
            break
        case NeedPermissionType.NEED_PERMISSION_LOCATION.rawValue:
            self.message = "text_status_message_need_permission_location"
            self.showButton = true
            break
        case NeedPermissionType.NEED_PERMISSION_CAMERA.rawValue:
            self.message = "text_status_message_need_permission_camera"
            self.showButton = true
            break
        default:
            self.message = "text_status_message_need_ble_enabled"
        }
    }
}

struct PermissionView: View {
    @Environment(\.colorScheme) var colorScheme
    @Environment(\.locale) var locale
    
    @ObservedObject var viewModel: CurrentPermissionModel
    
    init(type: Int) {
        self.viewModel = CurrentPermissionModel(type: type)
    }
    
    var body: some View {
        GeometryReader { (geometry) in
            VStack(alignment: .center) {
                Image(systemName: "exclamationmark.triangle.fill").resizable().frame(width: geometry.size.width * 0.25, height: geometry.size.width * 0.25, alignment: .center).foregroundColor(Color.orange)
                Text(self.viewModel.message.localized(locale.identifier)).padding(.top, 48).padding(.bottom, self.viewModel.showButton ?  24 : geometry.size.height * 0.25).multilineTextAlignment(.center)
                if self.viewModel.showButton {
                    Button(action: {
                        DelegateManager.shared.goToSettings()
                        UIApplication.shared.open(URL(string: UIApplication.openSettingsURLString)!)
                    }) {
                        Text("text_button_app_permissions".localized(ConfigurationManager.shared.getLanguage())).bold()
                    }.padding(.bottom, geometry.size.height * 0.25)
                }
            }.padding(.horizontal, 14)
            .frame(width: geometry.size.width, height: geometry.size.height, alignment: .bottom)
        }.background(LinearGradient(gradient: Gradient(colors: [Color.orange, colorScheme == .dark ? Color.black.opacity(0.0) : Color.white.opacity(0.0)]), startPoint: .top, endPoint: .center)).edgesIgnoringSafeArea(.all)
    }
}

struct PermissionView_Previews: PreviewProvider {
    static var previews: some View {
        PermissionView(type: NeedPermissionType.NEED_ENABLE_BLE.rawValue).preferredColorScheme(.dark).environment(\.locale, Locale(identifier: "tr"))
    }
}
