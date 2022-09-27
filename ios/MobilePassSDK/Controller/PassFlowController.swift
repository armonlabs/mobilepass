//
//  PassController.swift
//  MobilePassSDK
//
//  Created by Erinc Cakir on 14.02.2021.
//

import Foundation
import SwiftUI
import UIKit

@available(iOS 13.0, *)
public class PassFlowController: UIViewController {
    private var contentView: UIHostingController<PassFlowView> = UIHostingController(rootView: PassFlowView(key: Date.init().description))
    
    public override func viewDidLoad() {
        super.viewDidLoad()
        
        addChild(contentView)
        contentView.view.translatesAutoresizingMaskIntoConstraints = false
        
        view.addSubview(contentView.view)
        
        setupConstraints()
    }
        
    
    public override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        DelegateManager.shared.onCancelled(dismiss: false)
        
        contentView.willMove(toParent: nil)
        contentView.view.removeFromSuperview()
        contentView.removeFromParent()
    }
    
    fileprivate func setupConstraints() {
        contentView.view.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        contentView.view.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        contentView.view.leftAnchor.constraint(equalTo: view.leftAnchor).isActive = true
        contentView.view.rightAnchor.constraint(equalTo: view.rightAnchor).isActive = true
    }
    
}
