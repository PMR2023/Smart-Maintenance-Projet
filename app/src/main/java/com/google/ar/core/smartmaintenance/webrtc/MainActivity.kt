package com.google.ar.core.smartmaintenance.webrtc

import android.Manifest
import android.content.Context
import android.icu.text.Transliterator
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import com.google.ar.core.smartmaintenance.java.common.helpers.Posicao
import com.google.ar.core.smartmaintenance.java.common.helpers.TapHelper
import com.google.ar.core.smartmaintenance.java.common.samplerender.SampleRender
import com.google.ar.core.smartmaintenance.kotlin.common.helpers.ARCoreSessionLifecycleHelper
import com.google.ar.core.smartmaintenance.kotlin.helloar.HelloArActivity
import com.google.ar.core.smartmaintenance.kotlin.helloar.HelloArRenderer
import com.google.ar.core.smartmaintenance.kotlin.helloar.HelloArView
import com.google.ar.core.smartmaintenance.kotlin.helloar.R
import com.google.ar.core.smartmaintenance.kotlin.helloar.databinding.ActivityMainBinding
import com.google.ar.core.smartmaintenance.webrtc.models.IceCandidateModel
import com.google.ar.core.smartmaintenance.webrtc.models.MessageModel
import com.google.ar.core.smartmaintenance.webrtc.utils.NewMessageInterface
import com.google.ar.core.smartmaintenance.webrtc.utils.PeerConnectionObserver
import com.google.ar.core.smartmaintenance.webrtc.utils.RTCAudioManager
import com.google.gson.Gson
import com.permissionx.guolindev.PermissionX
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.SessionDescription
import org.webrtc.SurfaceEglRenderer
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoFrame
import org.webrtc.VideoSink
import kotlin.random.Random

class MainActivity : HelloArActivity(), NewMessageInterface {

    lateinit var binding : ActivityMainBinding
    var userName:String?=null
    var socketRepository: SocketRepository?=null
    private var rtcClient : RTCClient?=null
    private val TAG = "CallActivity"
    var target:String = ""
    private val gson = Gson()
    private var isMute = false
    private var isCameraPause = false
    private val rtcAudioManager by lazy { RTCAudioManager.create(this) }
    private var isSpeakerMode = true

    /**  AR variables */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val randomNums = List(6) { Random.nextInt(0, 2) }
        var code = randomNums.map {
            if(it == 1) {
                Random.nextInt(65, 90).toChar()
            } else {
                Random.nextInt(0, 9).toString()
            }
        }.joinToString("")

        binding.codeTV.text = code

        PermissionX.init(this)
            .permissions(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
            ).request{ allGranted, _ ,_ ->
                if (allGranted){
                    init()
                } else {
                    Toast.makeText(this,"you should accept all permissions",Toast.LENGTH_LONG).show()
                }
            }

        /** AR initialization */
        // Setup ARCore session lifecycle helper and configuration.
        arCoreSessionHelper = ARCoreSessionLifecycleHelper(this)
        // If Session creation or Session.resume() fails, display a message and log detailed
        // information.
        arCoreSessionHelper.exceptionCallback =
            { exception ->
                val message =
                    when (exception) {
                        is UnavailableUserDeclinedInstallationException ->
                            "Please install Google Play Services for AR"
                        is UnavailableApkTooOldException -> "Please update ARCore"
                        is UnavailableSdkTooOldException -> "Please update this app"
                        is UnavailableDeviceNotCompatibleException -> "This device does not support AR"
                        is CameraNotAvailableException -> "Camera not available. Try restarting the app."
                        else -> "Failed to create AR session: $exception"
                    }
                Log.e(HelloArActivity.TAG, "ARCore threw an exception", exception)
                view.snackbarHelper.showError(this, message)
            }

        // Configure session features, including: Lighting Estimation, Depth mode, Instant Placement.
        arCoreSessionHelper.beforeSessionResume = ::configureSession
        lifecycle.addObserver(arCoreSessionHelper)

        // Set up the Hello AR renderer.
        renderer = HelloArRenderer(this)
        lifecycle.addObserver(renderer)

        // Set up Hello AR UI.
        view = HelloArView(this, binding)
        lifecycle.addObserver(view)
        //setContentView(view.root)

        // Sets up an example renderer using our HelloARRenderer.
        SampleRender(view.surfaceView, renderer, assets)

        depthSettings.onCreate(this)
        instantPlacementSettings.onCreate(this)

        val nextButton = findViewById<Button>(R.id.nextButton)
        val previousButton = findViewById<Button>(R.id.previousButton)
        val photoButton = findViewById<Button>(R.id.takePhotoButton)

        /*nextButton.setOnClickListener {
            toastAlerter("Clicked NEXT BUTTON")
        }
        previousButton.setOnClickListener {
            toastAlerter("Clicked PREVIOUS BUTTON")
            view.tapHelper.simulateTouch()
        }
        photoButton.setOnClickListener {
            toastAlerter("Clicked TAKE PHOTO BUTTON")
        }*/
    }

    private fun init(){
        userName = binding.codeTV.text.toString()
        socketRepository = SocketRepository(this)
        userName?.let { socketRepository?.initSocket(it) }
        rtcClient = RTCClient(application,userName!!,socketRepository!!, object : PeerConnectionObserver() {
            override fun onIceCandidate(p0: IceCandidate?) {
                super.onIceCandidate(p0)
                rtcClient?.addIceCandidate(p0)
                val candidate = hashMapOf(
                    "sdpMid" to p0?.sdpMid,
                    "sdpMLineIndex" to p0?.sdpMLineIndex,
                    "sdpCandidate" to p0?.sdp
                )

                socketRepository?.sendMessageToSocket(
                    MessageModel("ice_candidate",userName,target,candidate)
                )

            }

            override fun onAddStream(p0: MediaStream?) {
                super.onAddStream(p0)
                p0?.videoTracks?.get(0)?.addSink(binding.remoteView)
                Log.d(TAG, "onAddStream: $p0")

            }
        })
        rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)


        binding.apply {
            buttonExp.setOnClickListener {
                if (buttonExp.text.toString() == "Call") {
                    socketRepository?.sendMessageToSocket(
                        MessageModel(
                            "start_call", userName, targetKey.text.toString(), null
                        )
                    )
                    target = targetKey.text.toString()
                } else {
                    targetKey.visibility = View.VISIBLE
                    buttonExp.text = "Call"
                }
            }

            switchCameraButton.setOnClickListener {
                rtcClient?.switchCamera()
            }

            micButton.setOnClickListener {
                if (isMute){
                    isMute = false
                    micButton.setImageResource(R.drawable.ic_baseline_mic_off_24)
                }else{
                    isMute = true
                    micButton.setImageResource(R.drawable.ic_baseline_mic_24)
                }
                rtcClient?.toggleAudio(isMute)
            }

            videoButton.setOnClickListener {
                if (isCameraPause){
                    isCameraPause = false
                    videoButton.setImageResource(R.drawable.ic_baseline_videocam_off_24)
                }else{
                    isCameraPause = true
                    videoButton.setImageResource(R.drawable.ic_baseline_videocam_24)
                }
                rtcClient?.toggleCamera(isCameraPause)
            }

            audioOutputButton.setOnClickListener {
                if (isSpeakerMode){
                    isSpeakerMode = false
                    audioOutputButton.setImageResource(R.drawable.ic_baseline_hearing_24)
                    rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.EARPIECE)
                }else{
                    isSpeakerMode = true
                    audioOutputButton.setImageResource(R.drawable.ic_baseline_speaker_up_24)
                    rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)

                }

            }
            endCallButton.setOnClickListener {
                setCallLayoutGone()
                setStartScreenLayoutVisible()
                setIncomingCallLayoutGone()
                rtcClient?.endCall()
            }
        }

    }

    override fun onNewMessage(message: MessageModel) {
        Log.d(TAG, "onNewMessage: $message")
        when(message.type){
            "click" -> {
                Log.d("CLICK LOG", "ENTROU")
                try {
                    Log.d("CLICK LOG", "${gson.fromJson(gson.toJson(message.data), Posicao::class.java)}")
                    view.tapHelper.simulateTouch(gson.fromJson(gson.toJson(message.data), Posicao::class.java).x,
                        gson.fromJson(gson.toJson(message.data), Posicao::class.java).y)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            "call_response"->{
                if (message.data == "user is not online"){
                    //user is not reachable
                    runOnUiThread {
                        Toast.makeText(this,"user is not reachable",Toast.LENGTH_LONG).show()

                    }
                }else{
                    //we are ready for call, we started a call
                    runOnUiThread {
                        setStartScreenLayoutGone()
                        setCallLayoutVisible()
                        binding.apply {
                            //rtcClient?.initializeSurfaceView(localView)
                            //rtcClient?.initializeSurfaceView(remoteView)
                            rtcClient?.startLocalVideo(localView)
                            localView.visibility = View.GONE
                            Log.d(TAG, "$rtcClient")
                            rtcClient?.call(target)
                        }
                    }

                }
            }
            "answer_received" ->{

                val session = SessionDescription(
                    SessionDescription.Type.ANSWER,
                    message.data.toString()
                )
                rtcClient?.onRemoteSessionReceived(session)
                runOnUiThread {
                    binding.remoteViewLoading.visibility = View.GONE
                }
            }
            "offer_received" ->{
                runOnUiThread {
                    setIncomingCallLayoutVisible()
                    binding.incomingNameTV.text = "${message.name.toString()} is calling you"
                    binding.acceptButton.setOnClickListener {
                        setIncomingCallLayoutGone()
                        setCallLayoutVisible()
                        setStartScreenLayoutGone()

                        binding.apply {
                            rtcClient?.initializeSurfaceView(localView)
                            //rtcClient?.initializeSurfaceView(remoteView)
                            rtcClient?.startLocalVideo(localView)
                            remoteView.visibility = View.GONE
                            val tapHelper = TapHelper(this@MainActivity).also { localView.setOnTouchListener(it) }
                        }
                        val session = SessionDescription(
                            SessionDescription.Type.OFFER,
                            message.data.toString()
                        )
                        rtcClient?.onRemoteSessionReceived(session)
                        rtcClient?.answer(message.name!!)
                        target = message.name!!
                        binding.remoteViewLoading.visibility = View.GONE

                    }
                    binding.rejectButton.setOnClickListener {
                        setIncomingCallLayoutGone()
                    }

                }

            }


            "ice_candidate"->{
                try {
                    val receivingCandidate = gson.fromJson(gson.toJson(message.data),
                        IceCandidateModel::class.java)
                    rtcClient?.addIceCandidate(IceCandidate(receivingCandidate.sdpMid,
                        Math.toIntExact(receivingCandidate.sdpMLineIndex.toLong()),receivingCandidate.sdpCandidate))
                }catch (e:Exception){
                    e.printStackTrace()
                }
            }

        }
    }

    private fun setIncomingCallLayoutGone(){
        binding.incomingCallLayout.visibility = View.GONE
    }
    private fun setIncomingCallLayoutVisible() {
        binding.incomingCallLayout.visibility = View.VISIBLE
    }

    private fun setCallLayoutGone() {
        binding.callLayout.visibility = View.GONE
    }

    private fun setCallLayoutVisible() {
        binding.callLayout.visibility = View.VISIBLE
    }

    private fun setStartScreenLayoutGone() {
        binding.StartScreen.visibility = View.GONE
    }

    private fun setStartScreenLayoutVisible() {
        binding.StartScreen.visibility = View.VISIBLE
    }

}

class SmartRenderer(context : Context, attrs : AttributeSet) : GLSurfaceView(context, attrs), VideoSink {
    private val eglRenderer = SurfaceEglRenderer("eglRenderer")

    override fun surfaceCreated(holder: SurfaceHolder) {
        super.surfaceCreated(holder)
        //TODO(holder"Not yet implemented")
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        super.surfaceChanged(holder, format, w, h)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        super.surfaceDestroyed(holder)
    }

    override fun onFrame(p0: VideoFrame?) {
        this.eglRenderer.onFrame(p0)
        //TODO("Not yet implemented")
    }

}

