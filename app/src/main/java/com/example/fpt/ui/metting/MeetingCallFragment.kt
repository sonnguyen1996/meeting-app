package com.example.fpt.ui.metting

import android.graphics.*
import android.media.Image
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.demo.domain.domain.entities.ErrorResult
import com.example.demothesisfpteduvn.R
import com.example.demothesisfpteduvn.databinding.FragmentCreateMeetingBinding
import com.example.demothesisfpteduvn.databinding.FragmentMeetingCallBinding
import com.example.fpt.classifer.ClassifierResult
import com.example.fpt.classifer.EmotionTfLiteClassifier
import com.example.fpt.classifer.TfLiteClassifier
import com.example.fpt.mtcnn.Box
import com.example.fpt.mtcnn.MTCNN
import com.example.fpt.ui.base.BaseFragment
import com.google.common.util.concurrent.ListenableFuture
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

class MeetingCallFragment : BaseFragment<MeetingViewModel, FragmentMeetingCallBinding>() {

    override fun initView() {
    }

    override fun initData() {
    }
    override fun initActions() {
    }


    override fun provideLayoutId()= R.layout.fragment_meeting_call

    override fun onRequestError(errorResponse: ErrorResult) {
    }

    override fun provideViewModelClass() = MeetingViewModel::class.java

    override fun isNeedHideBottomBar()= true;

}