package com.example.AlumniPortal.service;

import com.example.AlumniPortal.dto.CommentRequest;
import com.example.AlumniPortal.dto.CommentResponse;
import com.example.AlumniPortal.dto.ForumPostRequest;
import com.example.AlumniPortal.dto.ForumPostResponse;
import com.example.AlumniPortal.entity.ForumPost;
import com.example.AlumniPortal.exception.ResourceNotFoundException;
import com.example.AlumniPortal.repository.ForumPostRepository;
import com.example.AlumniPortal.security.CustomUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ForumService {

    private final ForumPostRepository postRepository;
    private final CommentService commentService;

    public ForumService(ForumPostRepository postRepository,
                        CommentService commentService) {
        this.postRepository = postRepository;
        this.commentService = commentService;
    }

    public ForumPostResponse createPost(ForumPostRequest request,
                                        CustomUserDetails user) {

        ForumPost post = ForumPost.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .category(request.getCategory())
                .authorId(user.getUserId())
                .authorName(user.getUsername())
                .tags(request.getTags())
                .likedUserIds(new LinkedHashSet<>())
                .build();

        return mapToResponse(postRepository.save(post), user == null ? null : user.getUserId(), List.of());
    }

    public List<ForumPostResponse> getAllPosts(CustomUserDetails user) {
        return mapPosts(postRepository.findAllByOrderByCreatedAtDesc(), user);
    }

    public ForumPostResponse getPostById(Long id, CustomUserDetails user) {
        ForumPost post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        return mapToResponse(
                post,
                user == null ? null : user.getUserId(),
                commentService.getCommentsForPost(post.getId())
        );
    }

    @Transactional
    public ForumPostResponse toggleLike(Long id, CustomUserDetails user) {
        ForumPost post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        Set<Long> likedUserIds = post.getLikedUserIds();
        if (likedUserIds == null) {
            likedUserIds = new LinkedHashSet<>();
            post.setLikedUserIds(likedUserIds);
        }

        if (likedUserIds.contains(user.getUserId())) {
            likedUserIds.remove(user.getUserId());
        } else {
            likedUserIds.add(user.getUserId());
        }

        post.setLikesCount(likedUserIds.size());
        ForumPost savedPost = postRepository.save(post);

        return mapToResponse(
                savedPost,
                user.getUserId(),
                commentService.getCommentsForPost(savedPost.getId())
        );
    }

    public void deletePost(Long id) {
        postRepository.deleteById(id);
    }

    public CommentResponse addComment(Long postId,
                                      CommentRequest request,
                                      CustomUserDetails user) {
        request.setPostId(postId);
        return commentService.createComment(request, user);
    }

    public List<ForumPostResponse> getByCategory(String category, CustomUserDetails user) {
        return mapPosts(postRepository.findByCategoryIgnoreCaseOrderByCreatedAtDesc(category), user);
    }

    private List<ForumPostResponse> mapPosts(List<ForumPost> posts, CustomUserDetails user) {
        Long currentUserId = user == null ? null : user.getUserId();
        Map<Long, List<CommentResponse>> commentsByPostId = commentService.getCommentsForPosts(
                posts.stream().map(ForumPost::getId).toList()
        );

        return posts.stream()
                .map(post -> mapToResponse(
                        post,
                        currentUserId,
                        commentsByPostId.getOrDefault(post.getId(), List.of())
                ))
                .toList();
    }

    private ForumPostResponse mapToResponse(ForumPost post,
                                            Long currentUserId,
                                            List<CommentResponse> comments) {
        Set<Long> likedUserIds = post.getLikedUserIds() == null ? Set.of() : post.getLikedUserIds();

        return ForumPostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory())
                .authorName(post.getAuthorName())
                .likesCount(likedUserIds.size())
                .likedByCurrentUser(currentUserId != null && likedUserIds.contains(currentUserId))
                .createdAt(post.getCreatedAt())
                .tags(post.getTags())
                .comments(comments)
                .build();
    }
}
