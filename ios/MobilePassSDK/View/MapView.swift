//
//  MapView.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 10.02.2021.
//

import SwiftUI
import MapKit

@available(iOS 13.0, *)
struct MapView: View {
    @Environment(\.locale) var locale
    
    @State private var isPrecisedLocation = true
    
    fileprivate var checkPoint: ResponseAccessPointListGeoLocation?
    
    init(checkPoint: ResponseAccessPointListGeoLocation?) {
        self.checkPoint = checkPoint
        PassFlowManager.shared.addToStates(state: .RUN_ACTION_LOCATION_WAITING)
    }
    
    var body: some View {
        ZStack(alignment: .bottom) {
            MapViewContent(checkPoint: checkPoint, completion: { result in
                if case .success(_) = result {
                    // _ = distance
                    DelegateManager.shared.flowLocationValidated()
                }
            }, isPrecisedLocation: { result in
                if (result != $isPrecisedLocation.wrappedValue) {
                    self.isPrecisedLocation.toggle()
                }
            }).edgesIgnoringSafeArea(.all)
            GeometryReader { (geometry) in
                VStack {
                    Text(($isPrecisedLocation.wrappedValue ? "text_location_message" : "text_location_message_reduced").localized(locale.identifier)).foregroundColor(.white).fontWeight(.medium).font(.system(size: 14)).multilineTextAlignment(.center)
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

@available(iOS 13.0, *)
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
    public var isPrecised: (Bool) -> Void
    public var checkPoint: ResponseAccessPointListGeoLocation?
    
    public init(checkPoint: ResponseAccessPointListGeoLocation?, completion: @escaping (Result<Double, Error>) -> Void, isPrecisedLocation: @escaping (Bool) -> Void) {
        self.completion = completion
        self.checkPoint = checkPoint
        self.isPrecised = isPrecisedLocation
    }
    
    func makeUIView(context: Context) -> MKMapView {
        mapView.delegate = context.coordinator
        
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.distanceFilter = kCLDistanceFilterNone
        locationManager.startUpdatingLocation()
        
        mapView.setUserTrackingMode(.follow, animated: true)
        mapView.showsUserLocation = true
        
        if (checkPoint != nil && checkPoint!.la != nil && checkPoint!.lo != nil && checkPoint!.r != nil) {
            let location = CLLocationCoordinate2D(latitude: checkPoint!.la!, longitude: checkPoint!.lo!)
            
            addRadiusOverlay(forLocation: location, radius: CLLocationDistance(checkPoint!.r!))
            
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
        self.checkLocationServices()
        
        NotificationCenter.default.addObserver(self, selector: #selector(self.onMapDisappeared(notification:)), name: Notification.Name("OnMapDisappear"), object: nil)
    }
    
    @objc func onMapDisappeared(notification: Notification) {
        self.parent.onDisappeared()
    }
    
    public func mapView(_ mapView: MKMapView, regionDidChangeAnimated animated: Bool) {
        
    }
    
    public func mapView(_ mapView: MKMapView, didUpdate userLocation: MKUserLocation) {
        LogManager.shared.debug(message: "User location changed: \(userLocation.coordinate.latitude), \(userLocation.coordinate.longitude)")
        
        var isPrecised: Bool = true
        
        if #available(iOS 14.0, *) {
            switch locationManager.accuracyAuthorization {
            case .fullAccuracy:
                LogManager.shared.debug(message: "Location settings has full accuracy")
                break
            case .reducedAccuracy:
                LogManager.shared.debug(message: "Location settings has reduced accuracy")
                isPrecised = false
                break
            @unknown default:
                LogManager.shared.debug(message: "Location settings has unknown accuracy")
            }
        }
        
        if (isInitialLocation) {
            isInitialLocation = false
            mapView.zoomToLocation(mapView.userLocation.location)
        }
        
        if (!isLocationFound && self.parent.checkPoint != nil && self.parent.checkPoint!.la != nil && self.parent.checkPoint!.lo != nil && self.parent.checkPoint!.r != nil) {
            // TODO: false is added to handle 'precise location' and mock location conflict, check here
            if (userLocation.location?.altitude == 0 && !ConfigurationManager.shared.isMockLocationAllowed() && false) {
                LogManager.shared.info(message: "Mock location detected, so pass flow will be cancelled")
                DelegateManager.shared.onMockLocationDetected()
            } else {
                let pinLoc = CLLocationCoordinate2D(latitude: self.parent.checkPoint!.la!, longitude: self.parent.checkPoint!.lo!)
                let userLoc = CLLocationCoordinate2D(latitude: userLocation.coordinate.latitude, longitude: userLocation.coordinate.longitude)
                
                let distance = MKMapPoint(userLoc).distance(to: MKMapPoint(pinLoc))
                
                if (distance < CLLocationDistance(self.parent.checkPoint!.r!)) {
                    PassFlowManager.shared.addToStates(state: .RUN_ACTION_LOCATION_VALIDATED)
                    
                    LogManager.shared.info(message: "User is in validation area now, distance to center point: \(distance)m")
                    isLocationFound = true
                    parent.completion(.success(distance))
                } else {
                    LogManager.shared.debug(message: "User is \(distance)m away from validation point")
                    parent.isPrecised(isPrecised)
                }
            }
        }
    }
    
    func mapView(_ mapView: MKMapView, rendererFor overlay: MKOverlay) -> MKOverlayRenderer {
        if overlay is MKCircle {
            let circle = MKCircleRenderer(overlay: overlay)
            circle.strokeColor = UIColor.red
            circle.fillColor = UIColor(red: 255, green: 0, blue: 0, alpha: 0.1)
            circle.lineWidth = 1
            return circle
        } else {
            return MKOverlayRenderer()
        }
    }
    
    private func checkLocationServices() {
        guard CLLocationManager.locationServicesEnabled() else {
            PassFlowManager.shared.addToStates(state: .PROCESS_ACTION_LOCATION_NEED_ENABLED)
            LogManager.shared.info(message: "Location services disabled, needs to be changed in settings to continue")
            DelegateManager.shared.needPermission(type: NeedPermissionType.NEED_ENABLE_LOCATION_SERVICES, showMessage: true)
            return
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        if (CLLocationManager.locationServicesEnabled()) {
            LogManager.shared.info(message: "Location authorization status changed")
            processAuthorizationStatus(status: status)
        }
    }
    
    private func processAuthorizationStatus(status: CLAuthorizationStatus) {
        if (status != .authorizedAlways && status != .authorizedWhenInUse) {
            PassFlowManager.shared.addToStates(state: .PROCESS_ACTION_LOCATION_NEED_PERMISSION)
        }
        
        switch status {
        case .authorizedAlways, .authorizedWhenInUse:
            LogManager.shared.info(message: "Location Permission Status: Authorized")
            break
        case .restricted, .denied:
            LogManager.shared.info(message: "Location Permission Status: Denied or Restricted, needs to be changed in settings to continue")
            DelegateManager.shared.needPermission(type: NeedPermissionType.NEED_PERMISSION_LOCATION, showMessage: true)
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
