package com.kennyc.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import com.kennyc.multistateview.R

class MultiStateView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : FrameLayout(context, attrs, defStyle) {
    private var contentView: View? = null
    private var loadingView: View? = null
    private var noNetView: View? = null
    private var errorView: View? = null
    private var emptyView: View? = null
    private var customView: View? = null
    private var animateLayoutChanges: Boolean = false
    var isUseSysInstanceState: Boolean = true
    var listener: StateListener? = null

    var viewState: ViewState = ViewState.CONTENT
        set(value) {
            val previousField = field
            //if (value != previousField) {
            field = value
            setView(previousField)
            listener?.onStateChanged(value)
            //}//end of if
        }

    enum class ViewState {
        CONTENT,
        LOADING,
        NoNet,
        ERROR,
        EMPTY,
        CUSTOM
    }

    init {
        val inflater = LayoutInflater.from(getContext())
        val a = getContext().obtainStyledAttributes(attrs, R.styleable.MultiStateView)

        val loadingViewResId = a.getResourceId(R.styleable.MultiStateView_msv_loadingView, -1)
        if (loadingViewResId > -1) {
            val inflatedLoadingView = inflater.inflate(loadingViewResId, this, false)
            loadingView = inflatedLoadingView
            addView(inflatedLoadingView, inflatedLoadingView.layoutParams)
        }//end of if

        val noNetViewResId = a.getResourceId(R.styleable.MultiStateView_msv_noNetView, -1)
        if (noNetViewResId > -1) {
            val inflatedNoNetView = inflater.inflate(noNetViewResId, this, false)
            noNetView = inflatedNoNetView
            addView(inflatedNoNetView, inflatedNoNetView.layoutParams)
        }//end of if

        val emptyViewResId = a.getResourceId(R.styleable.MultiStateView_msv_emptyView, -1)
        if (emptyViewResId > -1) {
            val inflatedEmptyView = inflater.inflate(emptyViewResId, this, false)
            emptyView = inflatedEmptyView
            addView(inflatedEmptyView, inflatedEmptyView.layoutParams)
        }//end of if

        val errorViewResId = a.getResourceId(R.styleable.MultiStateView_msv_errorView, -1)
        if (errorViewResId > -1) {
            val inflatedErrorView = inflater.inflate(errorViewResId, this, false)
            errorView = inflatedErrorView
            addView(inflatedErrorView, inflatedErrorView.layoutParams)
        }//end of if

        val customViewResId = a.getResourceId(R.styleable.MultiStateView_msv_customView, -1)
        if (customViewResId > -1) {
            val inflatedCustomView = inflater.inflate(customViewResId, this, false)
            customView = inflatedCustomView
            addView(inflatedCustomView, inflatedCustomView.layoutParams)
        }//end of if

        val ordinal = a.getInt(R.styleable.MultiStateView_msv_viewState, ViewState.CONTENT.ordinal)
        run breaking@{
            ViewState.values().forEach {
                if(it.ordinal == ordinal){
                    viewState = it
                    return@breaking
                }//end of if
            }
        }
        animateLayoutChanges = a.getBoolean(R.styleable.MultiStateView_msv_animateViewChanges, false)
        a.recycle()
    }

    /**
     * Returns the [View] associated with the [ViewState]
     *
     * @param state The [ViewState] with to return the view for
     * @return The [View] associated with the [ViewState], null if no view is present
     */
    fun getView(state: ViewState): View? {
        return when (state) {
            ViewState.LOADING -> loadingView
            ViewState.CONTENT -> contentView
            ViewState.NoNet -> noNetView
            ViewState.EMPTY -> emptyView
            ViewState.ERROR -> errorView
            ViewState.CUSTOM -> customView
        }
    }

    /**
     * Sets the view for the given view state
     *
     * @param view          The [View] to use
     * @param state         The [ViewState]to set
     * @param switchToState If the [ViewState] should be switched to
     */
    fun setViewForState(view: View, state: ViewState, switchToState: Boolean = false) {
        when (state) {
            ViewState.LOADING -> {
                if (loadingView != null) removeView(loadingView)
                loadingView = view
                addView(view)
            }
            ViewState.NoNet -> {
                if (noNetView != null) removeView(noNetView)
                noNetView = view
                addView(view)
            }
            ViewState.EMPTY -> {
                if (emptyView != null) removeView(emptyView)
                emptyView = view
                addView(view)
            }
            ViewState.ERROR -> {
                if (errorView != null) removeView(errorView)
                errorView = view
                addView(view)
            }
            ViewState.CUSTOM -> {
                if (customView != null) removeView(customView)
                customView = view
                addView(view)
            }
            ViewState.CONTENT -> {
                if (contentView != null) removeView(contentView)
                contentView = view
                addView(view)
            }
        }

        if (switchToState) viewState = state
    }

    /**
     * Sets the [View] for the given [ViewState]
     *
     * @param layoutRes     Layout resource id
     * @param state         The [ViewState] to set
     * @param switchToState If the [ViewState] should be switched to
     */
    fun setViewForState(@LayoutRes layoutRes: Int, state: ViewState, switchToState: Boolean = false) {
        val view = LayoutInflater.from(context).inflate(layoutRes, this, false)
        setViewForState(view, state, switchToState)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (contentView == null) throw IllegalArgumentException("Content view is not defined")

        when (viewState) {
            ViewState.CONTENT -> setView(ViewState.CONTENT)
            else -> contentView?.visibility = View.GONE
        }
    }

    override fun onSaveInstanceState(): Parcelable? {
        if(true != isUseSysInstanceState)return null
        return when (val superState = super.onSaveInstanceState()) {
            null -> superState
            else -> SavedState(superState, viewState)
        }
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if(true != isUseSysInstanceState)return
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)
            viewState = state.state
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    /* All of the addView methods have been overridden so that it can obtain the content view via XML
     It is NOT recommended to add views into MultiStateView via the addView methods, but rather use
     any of the setViewForState methods to set views for their given ViewState accordingly */
    override fun addView(child: View) {
        if (isValidContentView(child)) contentView = child
        super.addView(child)
    }

    override fun addView(child: View, index: Int) {
        if (isValidContentView(child)) contentView = child
        super.addView(child, index)
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        if (isValidContentView(child)) contentView = child
        super.addView(child, index, params)
    }

    override fun addView(child: View, params: ViewGroup.LayoutParams) {
        if (isValidContentView(child)) contentView = child
        super.addView(child, params)
    }

    override fun addView(child: View, width: Int, height: Int) {
        if (isValidContentView(child)) contentView = child
        super.addView(child, width, height)
    }

    override fun addViewInLayout(child: View, index: Int, params: ViewGroup.LayoutParams): Boolean {
        if (isValidContentView(child)) contentView = child
        return super.addViewInLayout(child, index, params)
    }

    override fun addViewInLayout(child: View, index: Int, params: ViewGroup.LayoutParams, preventRequestLayout: Boolean): Boolean {
        if (isValidContentView(child)) contentView = child
        return super.addViewInLayout(child, index, params, preventRequestLayout)
    }

    /**
     * Checks if the given [View] is valid for the Content View
     *
     * @param view The [View] to check
     * @return
     */
    private fun isValidContentView(view: View): Boolean {
        return if (contentView != null && contentView !== view) {
            false
        } else view != loadingView && view != noNetView && view != errorView && view != emptyView && view != customView
    }

    fun isContentViewShow(): Boolean = viewState == ViewState.CONTENT

    /**
     * Shows the [View] based on the [ViewState]
     */
    private fun setView(previousState: ViewState) {
        when (viewState) {
            ViewState.LOADING -> {
                loadingView?.apply {
                    contentView?.visibility = View.GONE
                    noNetView?.visibility = View.GONE
                    errorView?.visibility = View.GONE
                    emptyView?.visibility = View.GONE
                    customView?.visibility = View.GONE

                    if (animateLayoutChanges) {
                        animateLayoutChange(getView(previousState))
                    } else {
                        visibility = View.VISIBLE
                    }
                }
            }
            ViewState.NoNet -> {
                noNetView?.apply {
                    contentView?.visibility = View.GONE
                    errorView?.visibility = View.GONE
                    emptyView?.visibility = View.GONE
                    loadingView?.visibility = View.GONE
                    customView?.visibility = View.GONE

                    if (animateLayoutChanges) {
                        animateLayoutChange(getView(previousState))
                    } else {
                        visibility = View.VISIBLE
                    }
                }
            }
            ViewState.EMPTY -> {
                emptyView?.apply {
                    contentView?.visibility = View.GONE
                    noNetView?.visibility = View.GONE
                    errorView?.visibility = View.GONE
                    loadingView?.visibility = View.GONE
                    customView?.visibility = View.GONE

                    if (animateLayoutChanges) {
                        animateLayoutChange(getView(previousState))
                    } else {
                        visibility = View.VISIBLE
                    }
                }
            }
            ViewState.ERROR -> {
                errorView?.apply {
                    contentView?.visibility = View.GONE
                    loadingView?.visibility = View.GONE
                    noNetView?.visibility = View.GONE
                    emptyView?.visibility = View.GONE
                    customView?.visibility = View.GONE

                    if (animateLayoutChanges) {
                        animateLayoutChange(getView(previousState))
                    } else {
                        visibility = View.VISIBLE
                    }
                }
            }
            ViewState.CUSTOM -> {
                customView?.apply {
                    contentView?.visibility = View.GONE
                    loadingView?.visibility = View.GONE
                    noNetView?.visibility = View.GONE
                    errorView?.visibility = View.GONE
                    emptyView?.visibility = View.GONE

                    if (animateLayoutChanges) {
                        animateLayoutChange(getView(previousState))
                    } else {
                        visibility = View.VISIBLE
                    }
                }
            }
            ViewState.CONTENT -> {
                contentView?.apply {
                    loadingView?.visibility = View.GONE
                    noNetView?.visibility = View.GONE
                    errorView?.visibility = View.GONE
                    emptyView?.visibility = View.GONE
                    customView?.visibility = View.GONE

                    if (animateLayoutChanges) {
                        animateLayoutChange(getView(previousState))
                    } else {
                        visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    /**
     * Animates the layout changes between [ViewState]
     *
     * @param previousView The view that it was currently on
     */
    private fun animateLayoutChange(previousView: View?) {
        if (previousView == null) {
            getView(viewState)?.visibility = View.VISIBLE
            return
        }

        ObjectAnimator.ofFloat(previousView, "alpha", 1.0f, 0.0f).apply {
            duration = 250L
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?) {
                    previousView.visibility = View.VISIBLE
                }

                override fun onAnimationEnd(animation: Animator) {
                    previousView.visibility = View.GONE
                    getView(viewState)?.apply {
                        visibility = View.VISIBLE
                        ObjectAnimator.ofFloat(this, "alpha", 0.0f, 1.0f).setDuration(250L).start()
                    }
                }
            })
        }.start()
    }

    interface StateListener {
        /**
         * Callback for when the [ViewState] has changed
         *
         * @param viewState The [ViewState] that was switched to
         */
        fun onStateChanged(viewState: ViewState)
    }

    private class SavedState : BaseSavedState {
        internal val state: ViewState

        constructor(superState: Parcelable, state: ViewState) : super(superState) {
            this.state = state
        }

        constructor(parcel: Parcel) : super(parcel) {
            state = parcel.readSerializable() as ViewState
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeSerializable(state)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(`in`: Parcel): SavedState {
                    return SavedState(`in`)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }
}