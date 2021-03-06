//
//  MapView.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 10.02.2021.
//

import SwiftUI
import MapKit

struct MapView: View {
    @Environment(\.locale) var locale
    
    fileprivate var checkPoint: ResponseAccessPointItemGeoLocation?
    
    init(checkPoint: ResponseAccessPointItemGeoLocation?) {
        self.checkPoint = checkPoint
    }
    
    var body: some View {
        ZStack(alignment: .bottom) {
            MapViewContent(checkPoint: checkPoint, completion: { result in
                if case let .success(distance) = result {
                    DelegateManager.shared.flowLocationValidated()
                }
            }).edgesIgnoringSafeArea(.all)
            GeometryReader { (geometry) in
                VStack {
                    Text("text_location_message".localized(locale.identifier)).foregroundColor(.white).fontWeight(.medium).multilineTextAlignment(.center)
                }
                .padding(.bottom, geometry.safeAreaInsets.bottom + 16)
                .padding(.horizontal, 14)
                .frame(minWidth: 0, idealWidth: geometry.size.width, maxWidth: geometry.size.width, minHeight: 0, idealHeight: 120 + geometry.safeAreaInsets.bottom, maxHeight: 120 + geometry.safeAreaInsets.bottom, alignment: .center)
                .background(Color.black.opacity(0.6))
                .position(x: geometry.size.width / 2, y: geometry.size.height - 60 + (geometry.safeAreaInsets.bottom))
            }
        }.onDisappear(perform: {
            NotificationCenter.default.post(name: Notification.Name("OnMapDisappear"), object: nil)
        })
    }
}

struct MapView_Previews: PreviewProvider {
    static var previews: some View {
        MapView(checkPoint: nil)
    }
}

struct MapViewContent: UIViewRepresentable {
    fileprivate let locationManager: CLLocationManager = CLLocationManager()
    private let mapView: MKMapView = MKMapView()
    
    func makeCoordinator() -> MapViewCoordinator {
        return MapViewCoordinator(parent: self)
    }
    
    public var completion: (Result<Double, Error>) -> Void
    public var checkPoint: ResponseAccessPointItemGeoLocation?
    
    public init(checkPoint: ResponseAccessPointItemGeoLocation?, completion: @escaping (Result<Double, Error>) -> Void) {
        self.completion = completion
        self.checkPoint = checkPoint
    }
    
    func makeUIView(context: Context) -> MKMapView {
        mapView.delegate = context.coordinator
        
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.distanceFilter = kCLDistanceFilterNone
        locationManager.startUpdatingLocation()
        
        mapView.setUserTrackingMode(.follow, animated: true)
        mapView.showsUserLocation = true
        
        if (checkPoint != nil) {
            let location = CLLocationCoordinate2D(latitude: checkPoint!.latitude, longitude: checkPoint!.longitude)
            
            addRadiusOverlay(forLocation: location, radius: CLLocationDistance(checkPoint!.radius))
            
            let annotation = MKPointAnnotation()
            annotation.coordinate = location
            mapView.addAnnotation(annotation)
        }
        
        return mapView
    }
    
    func addRadiusOverlay(forLocation location: CLLocationCoordinate2D, radius: CLLocationDistance) {
        mapView.addOverlay(MKCircle(center: location, radius: radius))
    }
    
    func removeRadiusOverlay(forLocation location: CLLocationCoordinate2D) {
        // Find exactly one overlay which has the same coordinates & radius to remove
        guard let overlays: [MKOverlay]? = mapView.overlays else { return }
        for overlay in overlays! {
            guard let circleOverlay = overlay as? MKCircle else { continue }
            let coord = circleOverlay.coordinate
            if coord.latitude == location.latitude &&
                coord.longitude == location.longitude {
                mapView.removeOverlay(circleOverlay)
                break
            }
        }
    }
    
    func updateUIView(_ uiView: MKMapView, context: Context) {
    }
    
    func onDisappeared() {
        self.mapView.showsUserLocation = false
        self.locationManager.stopUpdatingLocation()
    }
    
    typealias UIViewType = MKMapView
    
}

class MapViewCoordinator: NSObject, MKMapViewDelegate, CLLocationManagerDelegate {
    private var isInitialLocation: Bool = true
    private var isLocationFound: Bool = false
    private let locationManager: CLLocationManager = CLLocationManager()
    
    var parent: MapViewContent
    
    init(parent: MapViewContent) {
        self.parent = parent
        
        super.init()
        
        self.locationManager.delegate = self
        self.checkLocationPermission()
        
        NotificationCenter.default.addObserver(self, selector: #selector(self.onMapDisappeared(notification:)), name: Notification.Name("OnMapDisappear"), object: nil)
    }
    
    @objc func onMapDisappeared(notification: Notification) {
        self.parent.onDisappeared()
    }
    
    public func mapView(_ mapView: MKMapView, regionDidChangeAnimated animated: Bool) {
        
    }
    
    public func mapView(_ mapView: MKMapView, didUpdate userLocation: MKUserLocation) {
        LogManager.shared.debug(message: "User location changed: \(userLocation.coordinate.latitude), \(userLocation.coordinate.longitude)")
        
        if (isInitialLocation) {
            isInitialLocation = false
            mapView.zoomToLocation(mapView.userLocation.location)
        }
        
        if (!isLocationFound && self.parent.checkPoint != nil) {
            if (userLocation.location?.altitude == 0 && !ConfigurationManager.shared.isMockLocationAllowed()) {
                DelegateManager.shared.onMockLocationDetected()
            } else {
                let pinLoc = CLLocationCoordinate2D(latitude: self.parent.checkPoint!.latitude, longitude: self.parent.checkPoint!.longitude)
                let userLoc = CLLocationCoordinate2D(latitude: userLocation.coordinate.latitude, longitude: userLocation.coordinate.longitude)
                
                let distance = MKMapPoint(userLoc).distance(to: MKMapPoint(pinLoc))
                
                if (distance < CLLocationDistance(self.parent.checkPoint!.radius)) {
                    isLocationFound = true
                    parent.completion(.success(distance))
                } else {
                    LogManager.shared.debug(message: "Distance: \(distance)")
                }
            }
        }
    }
    
    func mapView(_ mapView: MKMapView, rendererFor overlay: MKOverlay) -> MKOverlayRenderer! {
        if overlay is MKCircle {
            let circle = MKCircleRenderer(overlay: overlay)
            circle.strokeColor = UIColor.red
            circle.fillColor = UIColor(red: 255, green: 0, blue: 0, alpha: 0.1)
            circle.lineWidth = 1
            return circle
        } else {
            return nil
        }
    }
    
    private func checkLocationPermission() {
        guard CLLocationManager.locationServicesEnabled() else {
            LogManager.shared.info(message: "Location services disabled, needs to be changed in settings to continue")
            DelegateManager.shared.needLocationServicesEnabled()
            return
        }
        
        let status: CLAuthorizationStatus = CLLocationManager.authorizationStatus()
        self.processAuthorizationStatus(status: status)
    }
    
    func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        LogManager.shared.info(message: "Location authorization status changed")
        processAuthorizationStatus(status: status)
    }
    
    private func processAuthorizationStatus(status: CLAuthorizationStatus) {
        switch status {
        case .authorizedAlways, .authorizedWhenInUse:
            LogManager.shared.info(message: "Location Permission Status: Authorized")
            break
        case .restricted, .denied:
            LogManager.shared.info(message: "Location Permission Status: Denied or Restricted, needs to be changed in settings to continue")
            DelegateManager.shared.needPermissionLocation()
            break
        case .notDetermined:
            LogManager.shared.info(message: "Location Permission Status: Not Determined, request now!")
            locationManager.requestWhenInUseAuthorization()
            break
        default:
            LogManager.shared.info(message: "Location Permission Status: Unknown!")
        }
    }
    
}

extension MKMapView {
    func zoomToLocation(_ location: CLLocation?) {
        guard let coordinate = location?.coordinate else { return }
        let region = MKCoordinateRegion(center: coordinate, latitudinalMeters: 1000, longitudinalMeters: 1000)
        setRegion(region, animated: true)
    }
}
