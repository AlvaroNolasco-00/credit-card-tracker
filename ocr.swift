import Vision
import Cocoa
import Foundation

guard CommandLine.arguments.count > 1 else {
    print("Usage: swift ocr.swift <image_path>")
    exit(1)
}

let imagePath = CommandLine.arguments[1]
let url = URL(fileURLWithPath: imagePath)

guard let img = NSImage(contentsOf: url),
      let cgImage = img.cgImage(forProposedRect: nil, context: nil, hints: nil) else {
    print("Could not load image")
    exit(1)
}

let requestHandler = VNImageRequestHandler(cgImage: cgImage, options: .init())
let request = VNRecognizeTextRequest { (request, error) in
    if let error = error {
        print("Error: \(error)")
        return
    }
    
    guard let observations = request.results as? [VNRecognizedTextObservation] else {
        return
    }
    
    for observation in observations {
        let topCandidate = observation.topCandidates(1).first
        if let recognizedText = topCandidate?.string {
            print(recognizedText)
        }
    }
}

request.recognitionLevel = .accurate
request.recognitionLanguages = ["es-MX", "en-US"]
request.usesLanguageCorrection = true

do {
    try requestHandler.perform([request])
} catch {
    print("Error performing request: \(error)")
}
