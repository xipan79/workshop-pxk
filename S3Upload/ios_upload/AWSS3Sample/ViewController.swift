//
//  ViewController.swift
//  AWSS3Sample
//
//  Created by Dawei Tang 唐大伟 on 2021/1/29.
//

import UIKit
import AWSS3

class ViewController: UIViewController {
    
    private static let awsInit: Bool = {
        let accessKey = "When I was young"
        let secretKey = "I listen to the radio"
        let sessionToken = "Waiting for my favorite song"
        
        let credentialsProvider = AWSBasicSessionCredentialsProvider(accessKey: accessKey,
                                                                     secretKey: secretKey,
                                                                     sessionToken: sessionToken)
        let transferConfig = AWSS3TransferUtilityConfiguration()
        transferConfig.bucket = "we have joys"
        guard let configuration = AWSServiceConfiguration(region: AWSRegionType.EUCentral1,
                                                          credentialsProvider: credentialsProvider)
        else {
            return false
        }

        AWSS3TransferUtility.register(with: configuration,
                                      transferUtilityConfiguration: transferConfig,
                                      forKey: "we have fun") { (error) in
            if let error = error {
                print(error)
                assert(false, "we have seasons in the sun")
            }
        }
        return true
    }()
    
    private var trans: AWSS3TransferUtility {
        .s3TransferUtility(forKey: "we have fun")!
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        if Self.awsInit {
            self.upload()
        }
    }
    
    private func upload() {
        let expression = AWSS3TransferUtilityUploadExpression()
        expression.progressBlock = { (task, progress) in
            print("\(#function) - \(#line)")
            print(task)
            print(progress)
        }
        guard let image = UIImage(named: "upload"),
              let data = image.jpegData(compressionQuality: 0.9)
        else {
            assert(false, "data invalid")
        }
        self.trans
            .uploadData(data,
                        key: "",
                        contentType: "",
                        expression: expression)
            { (task, error) in
                print("\(#function) - \(#line)")
                print(task)
                if let error = error {
                    print(error)
                }
            }
            .continueWith
            { (awsTask) -> Any? in
                if let error = awsTask.error {
                    print("\(#function) - \(#line)")
                    print(error)
                }
                if let result = awsTask.result {
                    print("\(#function) - \(#line)")
                    print(result)
                }
                return nil
            }
    }
}

