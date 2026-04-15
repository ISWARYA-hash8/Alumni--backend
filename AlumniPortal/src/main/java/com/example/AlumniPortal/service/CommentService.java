package com.example.AlumniPortal.service;

import com.example.AlumniPortal.dto.CommentRequest;
import com.example.AlumniPortal.dto.CommentResponse;
import com.example.AlumniPortal.entity.Comment;
import com.example.AlumniPortal.entity.ForumPost;
import com.example.AlumniPortal.entity.User;
import com.example.AlumniPortal.exception.BadRequestException;
import com.example.AlumniPortal.exception.ResourceNotFoundException;
import com.example.AlumniPortal.repository.AlumniProfileRepository;
import com.example.AlumniPortal.repository.CommentRepository;
import com.example.AlumniPortal.repository.ForumPostRepository;
import com.example.AlumniPortal.repository.UserRepository;
import com.example.AlumniPortal.security.CustomUserDetails;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final ForumPostRepository forumPostRepository;
    private final UserRepository userRepository;
    private final AlumniProfileRepository alumniProfileRepository;

    public CommentService(CommentRepository commentRepository,
                          ForumPostRepository forumPostRepository,
                          UserRepository userRepository,
                          AlumniProfileRepository alumniProfileRepository) {
        this.commentRepository = commentRepository;
        this.forumPostRepository = forumPostRepository;
        this.userRepository = userRepository;
        this.alumniProfileRepository = alumniProfileRepository;
    }

    public CommentResponse createComment(CommentRequest request, CustomUserDetails user) {
        if (request.getPostId() == null) {
            throw new BadRequestException("Post id is required");
        }

        ForumPost post = forumPostRepository.findById(request.getPostId())
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        Comment comment = Comment.builder()
                .postId(post.getId())
                .userId(user.getUserId())
                .content(request.getContent().trim())
                .build();

        return mapToResponse(commentRepository.save(comment), getAuthorNamesByUserId(List.of(comment)));
    }

    public List<CommentResponse> getCommentsForPost(Long postId) {
        if (!forumPostRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post not found");
        }

        return getCommentsForPosts(Collections.singleton(postId))
                .getOrDefault(postId, List.of());
    }

    public Map<Long, List<CommentResponse>> getCommentsForPosts(Collection<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Map.of();
        }

        List<Comment> comments = commentRepository.findByPostIdInOrderByCreatedAtAsc(postIds);
        Map<Long, String> authorNamesByUserId = getAuthorNamesByUserId(comments);

        return comments.stream()
                .map(comment -> mapToResponse(comment, authorNamesByUserId))
                .collect(Collectors.groupingBy(CommentResponse::getPostId));
    }

    public void deleteComment(Long id, CustomUserDetails user) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole());
        if (!isAdmin && !comment.getUserId().equals(user.getUserId())) {
            throw new AccessDeniedException("You cannot delete this comment");
        }

        commentRepository.delete(comment);
    }

    private CommentResponse mapToResponse(Comment comment, Map<Long, String> authorNamesByUserId) {
        String authorName = authorNamesByUserId.getOrDefault(comment.getUserId(), "Unknown user");

        return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPostId())
                .userId(comment.getUserId())
                .content(comment.getContent())
                .authorName(authorName)
                .createdAt(comment.getCreatedAt())
                .build();
    }

    private Map<Long, String> getAuthorNamesByUserId(List<Comment> comments) {
        List<Long> userIds = comments.stream()
                .map(Comment::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, User> usersById = userRepository.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return userIds.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        userId -> alumniProfileRepository.findByUserId(userId)
                                .map(profile -> profile.getName())
                                .filter(name -> name != null && !name.isBlank() && !"Not Updated Yet".equalsIgnoreCase(name))
                                .orElseGet(() -> {
                                    User user = usersById.get(userId);
                                    if (user == null) {
                                        return "Unknown user";
                                    }

                                    if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
                                        return user.getDisplayName();
                                    }

                                    return user.getEmail();
                                })
                ));
    }
}
