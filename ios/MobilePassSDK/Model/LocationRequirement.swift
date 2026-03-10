//
//  LocationRequirement.swift
//  MobilePassSDK
//
//  Created by Mobile Pass SDK
//

import Foundation

public struct LocationRequirement {
    public var latitude: Double
    public var longitude: Double
    public var radius: Int  // meters
    
    init(latitude: Double, longitude: Double, radius: Int) {
        self.latitude = latitude
        self.longitude = longitude
        self.radius = radius
    }
}

