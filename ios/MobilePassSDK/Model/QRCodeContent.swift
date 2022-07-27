//
//  QRCodeself.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 18.02.2021.
//

import Foundation

struct QRCodeContent: Codable {
    var accessPointId:  String?
    var terminals:      [ResponseAccessPointListTerminal]?
    var qrCode:         ResponseAccessPointListQRCode?
    var geoLocation:    ResponseAccessPointListGeoLocation?
    var clubInfo:       ResponseAccessPointListClubInfo?
    var valid:          Bool = false
    
    init(accessPointId: String?, terminals: [ResponseAccessPointListTerminal]?, qrCode: ResponseAccessPointListQRCode?, geoLocation: ResponseAccessPointListGeoLocation?, clubInfo: ResponseAccessPointListClubInfo?) {
        self.accessPointId  = accessPointId
        self.terminals      = terminals
        self.qrCode         = qrCode
        self.geoLocation    = geoLocation
        self.clubInfo       = clubInfo
        
        self.valid = self.validateQRCodeContent()
    }
    
    private func validateQRCodeContent() -> Bool {
        if (self.qrCode == nil) {
            LogManager.shared.warn(message: "QR code content has missing configuration details", code: LogCodes.PASSFLOW_QRCODE_VALIDATION_CONFIG)
            return false;
        }
        
        if (self.qrCode!.q == nil || self.qrCode!.q!.isEmpty) {
            LogManager.shared.warn(message: "QR code content has invalid data", code: LogCodes.PASSFLOW_QRCODE_VALIDATION_DATA)
            return false;
        }
        
        if (self.qrCode!.i == nil || self.qrCode!.i!.isEmpty) {
            LogManager.shared.warn(message: "[\(self.qrCode!.q!)] QR code content has missing id value", code: LogCodes.PASSFLOW_QRCODE_VALIDATION_ID)
            return false;
        }
        
        if (self.qrCode!.d == nil || self.qrCode!.r == nil || self.qrCode!.h == nil || self.qrCode!.h!.isEmpty) {
            LogManager.shared.warn(message: "[\(self.qrCode!.q!)] QR code content has missing connection detail in direction, relay number or hardware id", code: LogCodes.PASSFLOW_QRCODE_VALIDATION_DOOR_DETAILS)
            return false
        }
        
        if (self.qrCode!.t == nil || !QRTriggerType.allCases.contains { $0.rawValue == self.qrCode!.t!.rawValue }) {
            LogManager.shared.warn(message: "[\(self.qrCode!.q!)] QR code content has invalid trigger type", code: LogCodes.PASSFLOW_QRCODE_VALIDATION_TRIGGERTYPE)
            return false
        }
        
        if (self.qrCode!.v == true && self.geoLocation != nil && (self.geoLocation!.la == nil || self.geoLocation!.lo == nil || self.geoLocation!.r == nil)) {
            LogManager.shared.warn(message: "[\(self.qrCode!.q!)] QR code content has invalid location details", code: LogCodes.PASSFLOW_QRCODE_VALIDATION_LOCATION)
            return false
        }
        
        if ((self.terminals ?? []).contains { $0.i == nil || $0.i!.isEmpty || $0.p == nil || $0.p!.isEmpty }) {
            LogManager.shared.warn(message: "[\(self.qrCode!.q!)] QR code content has invalid terminal details", code: LogCodes.PASSFLOW_QRCODE_VALIDATION_TERMINAL)
            return false
        }
        
        if (self.accessPointId == nil || self.accessPointId!.isEmpty) {
            LogManager.shared.warn(message: "[\(self.qrCode!.q!)] QR code content has invalid access point matching", code: LogCodes.PASSFLOW_QRCODE_VALIDATION_ACCESSPOINT_ID)
            return false
        }
        
        return true
    }
}
