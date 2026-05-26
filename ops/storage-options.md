# EduSphere LMS Storage Options

## Recommended mapping

| Requirement | Best Option |
| --- | --- |
| Full Production LMS | AWS S3 |
| Free Beginner Setup | Cloudinary |
| Self Hosted | MinIO |
| Image Optimization | Cloudinary |
| Kubernetes Ready | MinIO |
| Large Video Streaming | AWS S3 |

## Current repo status

- `local` storage: fully supported
- `cloudinary` storage: wired into assignment file upload flow
- `minio` and `s3`: not implemented yet, but the provider abstraction is now in place for the next phase

## Current Cloudinary env vars

```text
STORAGE_PROVIDER=cloudinary
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
CLOUDINARY_FOLDER=edusphere-lms
```
