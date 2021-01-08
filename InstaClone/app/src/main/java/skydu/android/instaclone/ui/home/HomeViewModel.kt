package skydu.android.instaclone.ui.home

import androidx.lifecycle.*
import skydu.android.instaclone.data.repository.PostRepository
import skydu.android.instaclone.data.repository.UserRepository
import skydu.android.instaclone.data.repository.model.DataResult
import skydu.android.instaclone.data.repository.model.PostViewData

class HomeViewModel : ViewModel() {
    private val userRepository: UserRepository = UserRepository()

    private val triggerLogOut = MutableLiveData<Unit>()

    private val toggleLike: MutableLiveData<LikeData> = MutableLiveData()

    private val postRepository: PostRepository = PostRepository()

    private val page: MutableLiveData<Int> = MutableLiveData()

    private var isLoading = false

    init {
        page.postValue(1)
    }

    val posts: LiveData<DataResult<PostResult>> =
        page.switchMap { page ->
            postRepository.fetchHomePostList(page).switchMap {
                liveData {
                    isLoading = it.state == DataResult.State.LOADING
                    if (it.state == DataResult.State.UNAUTHORIZED) {
                        triggerLogout()
                    } else {
                        emit(
                            DataResult(
                                it.state,
                                PostResult(page == 1, it.data ?: emptyList()),
                                it.errorMessage,
                            )
                        )
                    }
                }
            }
        }

    val like: LiveData<DataResult<LikeData>> = toggleLike.switchMap { likeData ->
        postRepository.doToggleLike(likeData.postId).switchMap {
            liveData {
                if (it.state == DataResult.State.UNAUTHORIZED) {
                    triggerLogout()
                } else {
                    emit(
                        DataResult(
                            it.state,
                            likeData,
                            it.errorMessage
                        )
                    )
                }
            }
        }
    }

    val loggedOutEvent = triggerLogOut.switchMap {
        userRepository.doLogout()
    }


    fun triggerLogout() {
        triggerLogOut.value = Unit
    }

    fun loadNextPage() {
        if (!isLoading) {
            page.postValue((page.value ?: 1) + 1)
        }
    }

    fun onLikeClicked(it: PostViewData) {
        toggleLike.postValue(LikeData(it.id, it.is_liked))
    }


    class LikeData(val postId: Int, val isCurrentlyLiked: Boolean)
    class PostResult(val firstPage: Boolean, val list: List<PostViewData>)
}